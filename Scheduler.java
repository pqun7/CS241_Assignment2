import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Scheduler {
    public static class Step {
        public final int time;
        public final String running;
        public final int tier;
        public final String tier1Queue;
        public final String tier2Queue;
        public final String event;

        Step(int time, String running, int tier, String tier1Queue, String tier2Queue, String event) {
            this.time = time;
            this.running = running;
            this.tier = tier;
            this.tier1Queue = tier1Queue;
            this.tier2Queue = tier2Queue;
            this.event = event;
        }
    }

    public static class Result {
        public final String algorithm;
        public final List<Process> processes;
        public final List<Step> steps = new ArrayList<Step>();
        public final List<String> timeline = new ArrayList<String>();
        public int busyTime;
        public int contextSwitchTime;

        Result(String algorithm, List<Process> processes) {
            this.algorithm = algorithm;
            this.processes = processes;
        }

        public double averageWaiting() {
            return average(0);
        }

        public double averageTurnaround() {
            return average(1);
        }

        public double averageResponse() {
            return average(2);
        }

        private double average(int metric) {
            double total = 0;
            for (Process p : processes) {
                total += metric == 0 ? p.waitingTime() : metric == 1 ? p.turnaroundTime() : p.responseTime();
            }
            return total / processes.size();
        }
    }

    private static final Comparator<Process> SRTF_ORDER = new Comparator<Process>() {
        public int compare(Process a, Process b) {
            if (a.remainingTime != b.remainingTime) return a.remainingTime - b.remainingTime;
            if (a.arrivalTime != b.arrivalTime) return a.arrivalTime - b.arrivalTime;
            return a.id.compareTo(b.id);
        }
    };

    public static List<Process> copyOf(List<Process> source) {
        List<Process> result = new ArrayList<Process>();
        for (Process p : source) result.add(p.copy());
        return result;
    }

    public static Result srtf(List<Process> input, int contextSwitch) {
        List<Process> processes = copyOf(input);
        Result result = new Result("Preemptive SJF (SRTF)", processes);
        int time = 0;
        String previous = null;
        while (!allFinished(processes)) {
            Process current = chooseSrtf(processes, time);
            if (current == null) {
                result.timeline.add("IDLE");
                result.steps.add(new Step(time, "IDLE", 0, "", "", "CPU idle"));
                time++;
                previous = null;
                continue;
            }
            if (previous != null && !previous.equals(current.id) && contextSwitch > 0) {
                for (int i = 0; i < contextSwitch; i++) {
                    result.timeline.add("CS");
                    result.steps.add(new Step(time, "CS", 0, "", "", "Context switch"));
                    result.contextSwitchTime++;
                    time++;
                }
                current = chooseSrtf(processes, time);
                if (current == null) {
                    previous = null;
                    continue;
                }
            }
            if (current.firstStartTime < 0) current.firstStartTime = time;
            result.timeline.add(current.id);
            result.steps.add(new Step(time, current.id, 0, "", "", "Executing"));
            current.remainingTime--;
            result.busyTime++;
            time++;
            if (current.remainingTime == 0) current.completionTime = time;
            previous = current.id;
        }
        return result;
    }

    private static Process chooseSrtf(List<Process> processes, int time) {
        Process selected = null;
        for (Process p : processes) {
            if (p.arrivalTime <= time && p.remainingTime > 0 &&
                    (selected == null || SRTF_ORDER.compare(p, selected) < 0)) selected = p;
        }
        return selected;
    }

    public static Result acs(List<Process> input, int quantum, int contextSwitch, int agingInterval) {
        List<Process> processes = copyOf(input);
        Result result = new Result("Adaptive Container Scheduler", processes);
        Deque<Process> tier1 = new ArrayDeque<Process>();
        List<Process> tier2 = new ArrayList<Process>();
        Set<String> admitted = new HashSet<String>();
        int time = 0;
        int quantumUsed = 0;
        Process current = null;
        String previous = null;
        while (!allFinished(processes)) {
            admit(processes, time, tier1, admitted);
            updateAging(tier2, time, agingInterval);
            if (current == null || current.remainingTime == 0 ||
                    (current.tier == 1 && quantumUsed == quantum)) {
                if (current != null && current.remainingTime > 0 && current.tier == 1 && quantumUsed == quantum) {
                    current.tier = 2;
                    current.tier2EntryTime = time;
                    tier2.add(current);
                    result.steps.add(new Step(time, "-", 2, queueText(tier1), queueText(tier2), current.id + " demoted to Tier 2"));
                }
                current = selectNext(tier1, tier2);
                quantumUsed = 0;
                if (current == null) {
                    result.timeline.add("IDLE");
                    result.steps.add(new Step(time, "IDLE", 0, queueText(tier1), queueText(tier2), "CPU idle"));
                    time++;
                    previous = null;
                    continue;
                }
                if (previous != null && !previous.equals(current.id) && contextSwitch > 0) {
                    for (int i = 0; i < contextSwitch; i++) {
                        admit(processes, time, tier1, admitted);
                        result.timeline.add("CS");
                        result.steps.add(new Step(time, "CS", 0, queueText(tier1), queueText(tier2), "Context switch"));
                        result.contextSwitchTime++;
                        time++;
                    }
                }
            }
            if (current.firstStartTime < 0) current.firstStartTime = time;
            result.timeline.add(current.id);
            result.steps.add(new Step(time, current.id, current.tier, queueText(tier1), queueText(tier2),
                    current.tier == 1 ? "Tier 1 execution" : "Tier 2 execution"));
            current.remainingTime--;
            result.busyTime++;
            if (current.tier == 1) quantumUsed++;
            for (Process p : tier2) if (p != current) p.tier2WaitingTime++;
            time++;
            if (current.remainingTime == 0) {
                current.completionTime = time;
                tier2.remove(current);
                current = null;
                previous = current == null ? result.timeline.get(result.timeline.size() - 1) : current.id;
            } else if (current.tier == 1 && quantumUsed == quantum) {
                previous = current.id;
            } else {
                previous = current.id;
            }
        }
        return result;
    }

    private static Process selectNext(Deque<Process> tier1, List<Process> tier2) {
        if (!tier1.isEmpty()) return tier1.removeFirst();
        Process selected = null;
        for (Process p : tier2) {
            if (selected == null || p.effectivePriority < selected.effectivePriority ||
                    (p.effectivePriority == selected.effectivePriority && p.tier2EntryTime < selected.tier2EntryTime) ||
                    (p.effectivePriority == selected.effectivePriority && p.tier2EntryTime == selected.tier2EntryTime &&
                            p.id.compareTo(selected.id) < 0)) selected = p;
        }
        return selected;
    }

    private static void admit(List<Process> processes, int time, Deque<Process> tier1, Set<String> admitted) {
        for (Process p : processes) {
            if (p.arrivalTime <= time && p.remainingTime > 0 && admitted.add(p.id)) tier1.addLast(p);
        }
    }

    private static void updateAging(List<Process> tier2, int time, int interval) {
        for (Process p : tier2) {
            p.effectivePriority = Math.max(0, p.initialPriority - p.tier2WaitingTime / interval);
        }
    }

    private static boolean allFinished(List<Process> processes) {
        for (Process p : processes) if (p.remainingTime > 0) return false;
        return true;
    }

    private static String queueText(Iterable<Process> queue) {
        StringBuilder text = new StringBuilder();
        for (Process p : queue) {
            if (text.length() > 0) text.append(",");
            text.append(p.id).append("(r=").append(p.remainingTime).append(",p=").append(p.effectivePriority).append(")");
        }
        return text.toString();
    }
}
