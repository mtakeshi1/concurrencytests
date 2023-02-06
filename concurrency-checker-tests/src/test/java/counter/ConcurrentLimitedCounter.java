package counter;

public interface ConcurrentLimitedCounter {
    int inc();

    int resetCount();
}
