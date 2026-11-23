import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

public class ReportGenerator {
    public static void writeCsv(String fileName, List<Process> input) throws IOException {
        PrintWriter out = new PrintWriter(fileName);
        out.println("Quantum,ContextSwitch,AverageWaiting,AverageTurnaround,AverageResponse");
        int[] quanta = {2, 3, 4, 5};
        int[] switches = {0, 1, 2};
        for (int cs : switches) for (int q : quanta) {
            Scheduler.Result r = Scheduler.acs(input, q, cs, 4);
            out.printf(Locale.US, "%d,%d,%.2f,%.2f,%.2f%n", q, cs,
                    r.averageWaiting(), r.averageTurnaround(), r.averageResponse());
        }
        out.close();
    }

    public static void writeReport(String fileName, Scheduler.Result sjf, Scheduler.Result acs,
                                   List<Process> input, int quantum, int contextSwitch) throws IOException {
        String[] lines = {
            "CS241 - Assignment 2: Cloud-Cluster CPU Scheduling",
            "",
            "Algorithms",
            "Preemptive SJF (SRTF) always selects the arrived process with the shortest remaining time.",
            "ACS uses Tier 1 round robin, Tier 2 non-preemptive priority scheduling, and aging.",
            "Tier 1 processes use Q=" + quantum + ". Aging interval is 4 time units.",
            "",
            "Scheduling assumptions",
            "A context switch consumes simulated CPU time and is charged only when changing processes.",
            "The initial dispatch after idle does not incur a context switch. Arrivals during a switch are admitted.",
            "Tier 2 is non-preemptive: a running Tier 2 process completes before another choice is made.",
            "Effective priority = max(0, initial priority - floor(Tier 2 waiting / aging interval)).",
            "",
            "Required test case results",
            "Algorithm: " + sjf.algorithm,
            "Average waiting: " + format(sjf.averageWaiting()) + "   Average turnaround: " + format(sjf.averageTurnaround()),
            "Average response: " + format(sjf.averageResponse()),
            "",
            "Algorithm: " + acs.algorithm,
            "Average waiting: " + format(acs.averageWaiting()) + "   Average turnaround: " + format(acs.averageTurnaround()),
            "Average response: " + format(acs.averageResponse()),
            "",
            "Per-process ACS metrics",
            "Process   Finish   Turnaround   Waiting   Response",
        };
        StringBuilder text = new StringBuilder();
        for (String line : lines) text.append(line).append("\n");
        for (Process p : acs.processes) {
            text.append(String.format(Locale.US, "%-9s %-8d %-12d %-9d %d%n", p.id, p.completionTime,
                    p.turnaroundTime(), p.waitingTime(), p.responseTime()));
        }
        text.append("\nACS timeline:\n");
        text.append(join(acs.timeline)).append("\n\nSJF timeline:\n").append(join(sjf.timeline));
        text.append("\n\nExperimental graphs (each # is approximately one time unit)\n");
        text.append("Context switch vs average waiting time:\n");
        appendChart(text, input, true, false);
        text.append("Quantum vs average waiting time:\n");
        appendChart(text, input, false, false);
        text.append("Context switch vs average turnaround time:\n");
        appendChart(text, input, true, true);
        text.append("Quantum vs average turnaround time:\n");
        appendChart(text, input, false, true);
        writeSimplePdf(fileName, text.toString());
    }

    private static void appendChart(StringBuilder text, List<Process> input, boolean byContextSwitch,
                                    boolean turnaround) {
        int[] values = byContextSwitch ? new int[] {0, 1, 2} : new int[] {2, 3, 4, 5};
        for (int value : values) {
            double total = 0;
            int count = 0;
            for (int cs = 0; cs <= 2; cs++) {
                for (int q = 2; q <= 5; q++) {
                    if ((byContextSwitch && cs != value) || (!byContextSwitch && q != value)) continue;
                    Scheduler.Result r = Scheduler.acs(input, q, cs, 4);
                    total += turnaround ? r.averageTurnaround() : r.averageWaiting();
                    count++;
                }
            }
            double average = total / count;
            text.append(String.format(Locale.US, "%s=%d %.2f ", byContextSwitch ? "CS" : "Q", value, average));
            int bars = (int) Math.round(average);
            for (int i = 0; i < bars; i++) text.append("#");
            text.append("\n");
        }
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String join(List<String> items) {
        StringBuilder result = new StringBuilder();
        for (String item : items) {
            if (result.length() > 0) result.append(" ");
            result.append(item);
        }
        return result.toString();
    }

    private static void writeSimplePdf(String fileName, String content) throws IOException {
        String[] rawLines = content.split("\n");
        StringBuilder stream = new StringBuilder("BT\n/F1 9 Tf\n50 750 Td\n");
        int line = 0;
        for (String raw : rawLines) {
            String safe = raw.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            stream.append("(").append(safe).append(") Tj\n0 -13 Td\n");
            line++;
            if (line == 52) {
                stream.append("ET\n");
                break;
            }
        }
        stream.append("ET\n");
        byte[] streamBytes = stream.toString().getBytes("ISO-8859-1");
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[6];
        offsets[1] = pdf.length(); pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        offsets[2] = pdf.length(); pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        offsets[3] = pdf.length(); pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");
        offsets[4] = pdf.length(); pdf.append("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        offsets[5] = pdf.length(); pdf.append("5 0 obj\n<< /Length ").append(streamBytes.length).append(" >>\nstream\n");
        String prefix = pdf.toString();
        FileOutputStream out = new FileOutputStream(new File(fileName));
        out.write(prefix.getBytes("ISO-8859-1"));
        out.write(streamBytes);
        out.write("endstream\nendobj\n".getBytes("ISO-8859-1"));
        int xref = prefix.getBytes("ISO-8859-1").length + streamBytes.length + "endstream\nendobj\n".length();
        StringBuilder tail = new StringBuilder("xref\n0 6\n0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) tail.append(String.format(Locale.US, "%010d 00000 n \n", offsets[i]));
        tail.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        out.write(tail.toString().getBytes("ISO-8859-1"));
        out.close();
    }
}
