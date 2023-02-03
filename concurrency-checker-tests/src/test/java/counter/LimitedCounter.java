package counter;

import concurrencytest.runtime.CheckpointRuntimeAccessor;

import java.util.concurrent.atomic.AtomicInteger;

public class LimitedCounter {

    private final int max;

    private static final int MIN = -1;

    private final AtomicInteger counter = new AtomicInteger(MIN);
    private final Object lock = new Object();
    private final AtomicInteger resetCounter = new AtomicInteger();

    public LimitedCounter(int max) {
        this.max = max;
    }

    public int inc() {
        int x = counter.incrementAndGet();
        if (x > max) {
            synchronized (lock) {
                int a = counter.get();
                CheckpointRuntimeAccessor.manualCheckpoint("before check, value was: " + a);
                if (a > max) {
                    counter.set(MIN);
                    resetCounter.incrementAndGet();
                }
                x = counter.incrementAndGet();
                CheckpointRuntimeAccessor.manualCheckpoint("after check, new value was: " + x);
            }
        }
        return x;
    }

}
