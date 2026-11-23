# CS241 Assignment 2 - Cloud-Cluster CPU Scheduling

This project implements Preemptive Shortest Job First (SRTF) and the Adaptive
Container Scheduler (ACS) in plain Java.

## Files

- `Process.java` - process data and metric calculations.
- `Scheduler.java` - SRTF and three-tier ACS simulations.
- `Main.java` - required and additional test cases, matrix output, and report generation.
- `ReportGenerator.java` - `results.csv` and a dependency-free PDF report.
- `results.csv` - generated experiment results.
- `Assignment_Report.pdf` - generated report.

## ACS rules

New processes enter Tier 1 (FIFO round robin). A process that uses all of its
quantum without finishing is demoted to Tier 2. Tier 2 selects the smallest
effective priority and is non-preemptive. Aging uses
`max(0, initialPriority - floor(tier2Waiting / 4))`.

The simulation charges context switching only when changing between processes,
not for the first dispatch after idle. Context-switch time consumes simulated
time, and arrivals during it are admitted. Ties are resolved by arrival time
and then process ID where applicable.

## Requirements and commands

JDK 8 or newer is sufficient. From PowerShell:

```powershell
cd C:\Users\Guest1\Desktop\Study\CS241_Assignment2
javac *.java
java Main
```

The command runs both required cases and an additional case, prints the
scheduling matrices, and regenerates `results.csv` and `Assignment_Report.pdf`.
The reference values in the assignment are treated as validation targets; the
program does not hardcode them.
