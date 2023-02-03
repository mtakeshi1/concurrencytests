package counter;

public class SyncCounter implements ConcurrentCounter {

    private volatile int c;

    @Override
    public synchronized void inc() {
        c++;
    }

    @Override
    public int get() {
        return c;
    }
}
