package concurrencytest.runner.statistics;

import java.util.concurrent.TimeUnit;

public interface RunStatistics {

    int numberOfRuns();

    long totalTimeNanos();

    int totalDepth();

    ImmutableRunStatistics snapShot();

    default RunStatistics sumWith(RunStatistics another) {
        return this.snapShot().sumWith(another.snapShot());
    }

    default String format() {
        double averageTime = ((double) totalTimeNanos()) / numberOfRuns();
        double averageDepth = ((double) totalDepth()) / numberOfRuns();
        var tt = new TimeWithUnit(averageTime, TimeUnit.NANOSECONDS);

        return "runs: %d, average depth: %.2f, average run time: %s".formatted(numberOfRuns(), averageDepth, tt.format());
    }

}
