package concurrencytest.runner.statistics;

public record ImmutableRunStatistics(int numberOfRuns, long totalTimeNanos, int totalDepth) implements RunStatistics {

    public static final RunStatistics ZERO = new ImmutableRunStatistics();

    public ImmutableRunStatistics() {
        this(0, 0, 0);
    }

    @Override
    public ImmutableRunStatistics snapShot() {
        return this;
    }

    @Override
    public RunStatistics sumWith(RunStatistics another) {
        return new ImmutableRunStatistics(
                this.numberOfRuns + another.numberOfRuns(),
                this.totalTimeNanos + another.totalTimeNanos(),
                this.totalDepth + another.totalDepth()
        );
    }
}
