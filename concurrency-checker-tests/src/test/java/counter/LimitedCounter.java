package counter;

import concurrencytest.runtime.CheckpointRuntimeAccessor;

import java.util.concurrent.atomic.AtomicInteger;

public class LimitedCounter implements ConcurrentLimitedCounter{

    private static final int MIN = -1;

    public LimitedCounter(int max) {
        this.max = max;
    }

    private final AtomicInteger resetCounter = new AtomicInteger();

    private final int max;
    private final AtomicInteger counter = new AtomicInteger(MIN);
    private final Object lock = new Object();

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
        return x % (max + 1);
    }

    @Override
    public int resetCount() {
        return resetCounter.get();
    }

}
