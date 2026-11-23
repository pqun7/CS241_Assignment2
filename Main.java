import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        List<Process> test1 = testCase1();
        List<Process> test2 = testCase2();
        runCase("Required Test Case 1", test1, 3, 0);
        runCase("Required Test Case 2", test2, 3, 1);
        runCase("Additional Test Case", additionalCase(), 2, 1);
        ReportGenerator.writeCsv("results.csv", test2);
        Scheduler.Result sjf = Scheduler.srtf(test2, 1);
        Scheduler.Result acs = Scheduler.acs(test2, 3, 1, 4);
        ReportGenerator.writeReport("Assignment_Report.pdf", sjf, acs, test2, 3, 1);
        System.out.println("\nGenerated results.csv and Assignment_Report.pdf");
    }

    private static void runCase(String title, List<Process> data, int quantum, int cs) {
        Scheduler.Result sjf = Scheduler.srtf(data, cs);
        Scheduler.Result acs = Scheduler.acs(data, quantum, cs, 4);
        System.out.println("\n" + title);
        printResult(sjf);
        printResult(acs);
        printMatrix(acs);
    }

    private static void printResult(Scheduler.Result result) {
        System.out.printf("%n%s%n", result.algorithm);
        System.out.println("Process Finish Turnaround Waiting Response");
        for (Process p : result.processes) {
            System.out.printf("%s      %d      %d          %d       %d%n", p.id, p.completionTime,
                    p.turnaroundTime(), p.waitingTime(), p.responseTime());
        }
        System.out.printf("Averages: waiting %.2f, turnaround %.2f, response %.2f%n",
                result.averageWaiting(), result.averageTurnaround(), result.averageResponse());
        System.out.println("Timeline: " + result.timeline);
    }

    private static void printMatrix(Scheduler.Result result) {
        System.out.println("ACS scheduling matrix");
        System.out.println("Time | Running | Tier | Tier 1 queue | Tier 2 queue | Event");
        for (Scheduler.Step step : result.steps) {
            System.out.printf("%4d | %-7s | %4d | %-12s | %-12s | %s%n", step.time, step.running,
                    step.tier, step.tier1Queue, step.tier2Queue, step.event);
        }
    }

    private static List<Process> testCase1() {
        List<Process> data = new ArrayList<Process>();
        data.add(new Process("P1", 0, 4, 2));
        data.add(new Process("P2", 1, 7, 3));
        data.add(new Process("P3", 3, 2, 1));
        return data;
    }

    private static List<Process> testCase2() {
        List<Process> data = new ArrayList<Process>();
        data.add(new Process("P1", 0, 5, 4));
        data.add(new Process("P2", 1, 6, 2));
        data.add(new Process("P3", 4, 4, 3));
        return data;
    }

    private static List<Process> additionalCase() {
        List<Process> data = new ArrayList<Process>();
        data.add(new Process("P1", 0, 1, 2));
        data.add(new Process("P2", 0, 7, 2));
        data.add(new Process("P3", 5, 2, 1));
        data.add(new Process("P4", 10, 3, 1));
        return data;
    }
}
