package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointRegister;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RuntimeState {

    CheckpointRegister checkpointRegister();

    int monitorIdFor(Object object);

    int lockIdFor(Lock lock);

    Collection<? extends ThreadState> allActors();

    Collection<ManagedThread> start(Object testInstance, Duration timeout)throws InterruptedException, TimeoutException;

    /**
     * Signal the given actor to advance tot he next checkpoint. The current thread waits for rendezvous for the given maxWaitTime.
     * This methos can either mutate the current instance of create a copy.
     *
     * @param selected the actor selected to resume
     * @param maxWaitTime max wait time for the rendezvous
     * @return the new state or 'this' with a mutated state
     * @throws TimeoutException if maxWaitTime passed without the threads reaching their destination
     */
    RuntimeState advance(ThreadState selected, Duration maxWaitTime) throws InterruptedException, TimeoutException;

    default Map<Integer, ThreadState> ownedMonitors() {
        Map<Integer, ThreadState> monitors = new HashMap<>();
        allActors().forEach(ts -> ts.ownedMonitors().forEach(mon -> monitors.put(mon.lockOrMonitorId(), ts)));
        return monitors;
    }

    default Map<Integer, ThreadState> lockedLocks() {
        Map<Integer, ThreadState> monitors = new HashMap<>();
        allActors().forEach(ts -> ts.ownedLocks().forEach(lock -> monitors.put(lock.lockOrMonitorId(), ts)));
        return monitors;
    }

    default Map<Integer, Collection<ThreadState>> threadsWaitingForMonitor() {
        return allActors().stream().filter(ts -> ts.waitingForMonitor().isPresent()).collect(Collectors.toMap(
                ts1 -> ts1.waitingForMonitor().get().lockOrMonitorId(),
                RuntimeState::singleton,
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }
        ));
    }

    static <E> ArrayList<E> singleton(E element) {
        ArrayList<E> list = new ArrayList<>();
        list.add(element);
        return list;
    }

    default Map<Integer, Collection<ThreadState>> threadsWaitingForLocks() {
        return allActors().stream().filter(ts -> ts.waitingForLock().isPresent()).collect(Collectors.toMap(
                ts1 -> ts1.waitingForLock().get().lockOrMonitorId(),
                RuntimeState::singleton,
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }
        ));
    }

    default Map<String, ThreadState> actorNamesToThreadStates() {
        Map<String, ThreadState> map = new HashMap<>();
        for (ThreadState state : allActors()) {
            map.put(state.actorName(), state);
        }
        return map;
    }

    default Stream<? extends ThreadState> runnableActors() {
        return allActors().stream().filter(e -> e.canProceed(this));
    }

    default boolean finished() {
        return allActors().stream().allMatch(ts -> ts.checkpoint() == checkpointRegister().taskFinishedCheckpoint().checkpointId());
    }

}
