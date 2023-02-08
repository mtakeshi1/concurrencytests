package counter;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounter implements ConcurrentCounter {

    private final AtomicInteger c = new AtomicInteger();

    @Override
    public void inc() {
        c.incrementAndGet();
    }

    @Override
    public int get() {
        return c.get();
    }
}
