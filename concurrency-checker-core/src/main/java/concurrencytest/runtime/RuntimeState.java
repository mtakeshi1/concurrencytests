package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.config.Configuration;
import concurrencytest.runtime.impl.MutableRuntimeState;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.thread.ThreadState;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

/**
 * Runtime state represents a particular state in the system.
 * It should have information on what the actors are, where are they parked, the path taken so far and the various ids for locks and monitors
 * to keep track of resources used to block threads.
 * <p>
 * Currently the only version is {@link MutableRuntimeState} but a unmodifiable one should be much better
 */
public interface RuntimeState {

    Configuration configuration();

    /**
     * The checkpoint register containing all the checkpoints that actors can reach
     */
    CheckpointRegister checkpointRegister();

    /**
     * Returns an int that can be used to identify monitors. Its akin to {@link System#identityHashCode(Object)} in that it is unique per object.
     */
    int monitorIdFor(Object object);

    /**
     * Same as {@link RuntimeState#monitorIdFor(Object)} except for {@link Lock}
     */
    int lockIdFor(Lock lock);

    /**
     * Returns a list of strings representing a trace of what threads reached which checkpoint. Should be used to try to find causes of error.
     */
    List<String> getExecutionPath();

    /**
     * @return all of the actors currently active
     */
    Collection<? extends ThreadState> allActors();

    /**
     * Starts this {@link RuntimeState} using the given Object as owner of the actors and using the given timeout as a maximum time to wait
     * for actors to reach a checkpoint.
     *
     * @return a collection of futures holding exceptions happening inside the actor methods.
     * @throws InterruptedException if this thread is interrupted while waiting for actors to reach their checkpoints
     * @throws TimeoutException     if the time to wait for actors to reach checkpoints exceeded the specified {@link Duration}
     */
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

    /**
     * If an error happens on actor methods, it will be contained here
     */
    Optional<Throwable> errorReported();

    /**
     * Collects all owned possibly blocking resources.
     * The returned Map is a snapshot of a particular state at a point in time. Modifications to the returned map
     * are not reflected anywhere.
     */
    default Map<BlockingResource, ? extends Collection<? extends ThreadState>> ownedResources() {
        Map<BlockingResource, Set<ThreadState>> owned = new HashMap<>();
        for (ThreadState ts : allActors()) {
            for (var resource : ts.ownedResources()) {
                owned.computeIfAbsent(resource, ignored -> new HashSet<>()).add(ts);
            }
        }
        return owned;
    }


    /**
     * Collects all actors and sort them into names
     */
    default Map<String, ThreadState> actorNamesToThreadStates() {
        Map<String, ThreadState> map = new HashMap<>();
        for (ThreadState state : allActors()) {
            map.put(state.actorName(), state);
        }
        return map;
    }

    /**
     * Returns a Stream of ThreadState of actors that are runnable - ie: not blocked by other actors and also not finished
     */
    default Stream<? extends ThreadState> runnableActors() {
        return allActors().stream().filter(e -> e.canProceed(this));
    }

    /**
     * @return true if all actors are finished (or about to finish)
     */
    default boolean finished() {
        return allActors().stream().allMatch(ts -> ts.checkpoint() == checkpointRegister().taskFinishedCheckpoint().checkpointId());
    }

    int getWaitCount(ThreadState actor, CheckpointDescription acquisitionPoint, int resourceId);

    boolean isNotifySignalAvailable(RuntimeState state, int resourceId);
}
