package concurrencytest.tree;

import java.util.List;
import java.util.Optional;

public interface ThreadState {

    String name();

    long id();

    long checkPointId();

    int loopCount();

    List<Integer> monitorsOwned();

    Optional<Integer> waitingForMonitor();

    Optional<Integer> objectWait();

    List<Integer> locksOwned();

    Optional<Integer> waitingForLocks();

    Optional<Integer> conditionWait();

    boolean runnable();

    boolean finished();

    Thread nativeThread();
}
