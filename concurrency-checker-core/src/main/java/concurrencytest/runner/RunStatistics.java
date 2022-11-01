package concurrencytest.runner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public record RunStatistics(AtomicInteger runs, AtomicLong accumulatedTimeNanos, AtomicInteger accumulatedDepth) {

    public RunStatistics() {
        this(new AtomicInteger(), new AtomicLong(), new AtomicInteger());
    }

    public RunStatistics(int runs, long time, int depthSum) {
        this(new AtomicInteger(runs), new AtomicLong(time), new AtomicInteger(depthSum));


    }

    public void record(long timeNanos, int depth) {
        runs.incrementAndGet();
        accumulatedTimeNanos.addAndGet(timeNanos);
        accumulatedDepth.addAndGet(depth);
    }

    public RunStatistics sum(RunStatistics run) {
        return new RunStatistics(
                this.runs.get() + run.runs().get(),
                this.accumulatedTimeNanos.get() + run.accumulatedTimeNanos.get(),
                this.accumulatedDepth.get() + run.accumulatedDepth.get()
        );
    }

    public record TimeWithUnit(double time, TimeUnit unit) {
        public static String unitName(TimeUnit unit) {
            return switch (unit) {
                case NANOSECONDS -> "ns";
                case MICROSECONDS -> "us";
                case MILLISECONDS -> "ms";
                case SECONDS -> "s";
                default -> "-";
            };
        }

        public String format() {
            double nom = time;
            int unitIndex = TimeUnit.NANOSECONDS.ordinal();
            while (nom > 1000 && unitIndex < TimeUnit.SECONDS.ordinal()) {
                nom /= 1000.0;
                unitIndex++;
            }
            return "%.2f%s".formatted(nom, unitName(TimeUnit.values()[unitIndex]));
        }

    }

    @Override
    public String toString() {
        double averageTime = ((double) accumulatedTimeNanos.get()) / runs.get();
        double averageDepth = ((double) accumulatedDepth.get()) / runs.get();
        var tt = new TimeWithUnit(averageTime, TimeUnit.NANOSECONDS);

        return "runs: %d, average depth: %.2f, averate run time: %s".formatted(runs.get(), averageDepth, tt.format());
    }

}
