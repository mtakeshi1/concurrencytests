package counter;

public class VolatileCounter implements ConcurrentCounter {

    private volatile int c;


    @Override
    public void inc() {
        c++;
    }

    @Override
    public int get() {
        return c;
    }
}
