package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.lock.BlockCause;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.thread.ManagedThread;
import concurrencytest.runtime.thread.ThreadState;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RuntimeState {

    CheckpointRegister checkpointRegister();

    int monitorIdFor(Object object);

    List<String> getExecutionPath();

    int lockIdFor(Lock lock);

    Collection<? extends ThreadState> allActors();

    Collection<Future<Throwable>> start(Object testInstance, Duration timeout) throws InterruptedException, TimeoutException;

    /**
     * Signal the given actor to advance tot he next checkpoint. The current thread waits for rendezvous for the given maxWaitTime.
     * This methos can either mutate the current instance of create a copy.
     *
     * @param selected    the actor selected to resume
     * @param maxWaitTime max wait time for the rendezvous
     * @return the new state or 'this' with a mutated state
     * @throws TimeoutException if maxWaitTime passed without the threads reaching their destination
     */
    RuntimeState advance(ThreadState selected, Duration maxWaitTime) throws InterruptedException, TimeoutException;

    Optional<Throwable> errorReported();

    default Map<BlockingResource, ? extends Collection<? extends ThreadState>> ownedResources() {
        Map<BlockingResource, Set<ThreadState>> owned = new HashMap<>();
        for (ThreadState ts : allActors()) {
            for (var resource : ts.ownedResources()) {
                owned.computeIfAbsent(resource, ignored -> new HashSet<>()).add(ts);
            }
        }
        return owned;
    }


    static <E> ArrayList<E> singleton(E element) {
        ArrayList<E> list = new ArrayList<>();
        list.add(element);
        return list;
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
