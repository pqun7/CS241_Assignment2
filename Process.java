public class Process {
    public final String id;
    public final int arrivalTime;
    public final int burstTime;
    public final int initialPriority;

    public int remainingTime;
    public int effectivePriority;
    public int firstStartTime = -1;
    public int completionTime = -1;
    public int tier = 1;
    public int tier2EntryTime = -1;
    public int tier2WaitingTime = 0;

    public Process(String id, int arrivalTime, int burstTime, int priority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.initialPriority = priority;
        this.remainingTime = burstTime;
        this.effectivePriority = priority;
    }

    public Process copy() {
        return new Process(id, arrivalTime, burstTime, initialPriority);
    }

    public int turnaroundTime() {
        return completionTime - arrivalTime;
    }

    public int waitingTime() {
        return turnaroundTime() - burstTime;
    }

    public int responseTime() {
        return firstStartTime - arrivalTime;
    }

    @Override
    public String toString() {
        return id;
    }
}
