package concurrencytest.runner.statistics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public record MutableRunStatistics(AtomicInteger runs, AtomicLong accumulatedTimeNanos, AtomicInteger accumulatedDepth) implements RunStatistics {

    public MutableRunStatistics() {
        this(new AtomicInteger(), new AtomicLong(), new AtomicInteger());
    }

    public MutableRunStatistics(int runs, long time, int depthSum) {
        this(new AtomicInteger(runs), new AtomicLong(time), new AtomicInteger(depthSum));
    }

    public synchronized void record(long timeNanos, int depth) {
        runs.incrementAndGet();
        accumulatedTimeNanos.addAndGet(timeNanos);
        accumulatedDepth.addAndGet(depth);
    }

    public synchronized RunStatistics reset() {
        RunStatistics oldValues = this.snapShot();
        runs.set(0);
        accumulatedTimeNanos.set(0);
        accumulatedDepth.set(0);
        return oldValues;
    }

    @Override
    public int numberOfRuns() {
        return runs.get();
    }

    @Override
    public long totalTimeNanos() {
        return accumulatedTimeNanos.get();
    }

    @Override
    public int totalDepth() {
        return accumulatedDepth.get();
    }

    @Override
    public synchronized ImmutableRunStatistics snapShot() {
        return new ImmutableRunStatistics(this.runs.get(), this.accumulatedTimeNanos.get(), this.accumulatedDepth.get());
    }

    @Override
    public String toString() {
        double averageTime = ((double) accumulatedTimeNanos.get()) / runs.get();
        double averageDepth = ((double) accumulatedDepth.get()) / runs.get();
        var tt = new TimeWithUnit(averageTime, TimeUnit.NANOSECONDS);

        return "runs: %d, average depth: %.2f, averate run time: %s".formatted(runs.get(), averageDepth, tt.format());
    }

}
