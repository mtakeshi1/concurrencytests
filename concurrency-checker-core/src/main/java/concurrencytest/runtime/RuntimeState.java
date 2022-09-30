package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.tree.ThreadState;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RuntimeState {

    CheckpointRegister checkpointRegister();

    int monitorIdFor(Object object);

    int lockIdFor(Lock lock);

    Collection<? extends ThreadState> allActors();

    Collection<ManagedThread> start();

    RuntimeState advance(ThreadState selected, Duration maxWaitTime) throws InterruptedException, TimeoutException;

    default Map<Integer, ThreadState> ownedMonitors() {
        Map<Integer, ThreadState> monitors = new HashMap<>();
        allActors().forEach(ts -> ts.ownedMonitors().forEach(mon -> monitors.put(mon, ts)));
        return monitors;
    }

    default Map<Integer, ThreadState> lockedLocks() {
        Map<Integer, ThreadState> monitors = new HashMap<>();
        allActors().forEach(ts -> ts.ownedLocks().forEach(lock -> monitors.put(lock, ts)));
        return monitors;

    }

    default Map<Integer, Collection<ThreadState>> threadsWaitingForMonitor() {
        return allActors().stream().filter(ts -> ts.waitingForMonitor().isPresent()).collect(Collectors.toMap(
                ts1 -> ts1.waitingForMonitor().get(),
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
                ts1 -> ts1.waitingForLock().get(),
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
