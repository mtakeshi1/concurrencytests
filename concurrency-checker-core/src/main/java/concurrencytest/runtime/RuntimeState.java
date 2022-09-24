package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.tree.ThreadState;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RuntimeState {

    CheckpointRegister checkpointRegister();

    int monitorIdFor(Object object);

    int lockIdFor(Lock lock);

    Collection<? extends ManagedThread> allActors();

    Map<Integer, ManagedThread> ownedMonitors();

    Map<Integer, Collection<ManagedThread>> threadsWaitingForMonitor();

    Map<Integer, ManagedThread> lockedLocks();

    Map<Integer, Collection<ManagedThread>> threadsWaitingForLocks();

    Map<ManagedThread, ThreadState> threadStates();

    Map<String, ManagedThread> actorNamesToThreads();

    Map<String, ThreadState> actorNamesToThreadStates();

    RuntimeState advance(ManagedThread selected, Duration duration) throws InterruptedException, TimeoutException;

    default Stream<? extends ManagedThread> runnableActors() {
        return threadStates().entrySet().stream().filter(e -> e.getValue().canProceed(this)).map(Map.Entry::getKey);
    }

    default boolean finished() {
        return threadStates().values().stream().allMatch(ThreadState::finished);
    }

}
