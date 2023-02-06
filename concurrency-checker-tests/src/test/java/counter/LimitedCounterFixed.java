package counter;

import java.util.concurrent.atomic.AtomicInteger;

public class LimitedCounterFixed implements ConcurrentLimitedCounter {

    private final int max;

    private static final int MIN = -1;

    private final AtomicInteger counter = new AtomicInteger(MIN);

    public LimitedCounterFixed(int max) {
        this.max = max;
    }

    public int inc() {
        return counter.incrementAndGet() % (max + 1);
    }

    public int resetCount() {
        return counter.get() / (max + 1);
    }

}
