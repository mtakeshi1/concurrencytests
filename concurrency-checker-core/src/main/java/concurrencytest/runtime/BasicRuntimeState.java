package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runner.CheckpointReachedCallback;
import concurrencytest.runtime.tree.ThreadState;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

public class BasicRuntimeState implements RuntimeState, CheckpointReachedCallback {

    private final CheckpointRegister register;

    private final Map<Object, Integer> monitorIds = new ConcurrentHashMap<>();
    private final Map<Lock, Integer> lockIds = new ConcurrentHashMap<>();

    private final AtomicInteger monitorLockSeed = new AtomicInteger();
    private final Collection<ThreadState> allActors;


    public BasicRuntimeState(CheckpointRegister register, Collection<ThreadState> allActors) {
        this.register = register;
        this.allActors = Collections.unmodifiableCollection(allActors);
    }

    @Override
    public void checkpointReached(ManagedThread managedThread, CheckpointReached checkpointReached, RuntimeState currentState) throws Exception {

    }

    @Override
    public CheckpointRegister checkpointRegister() {
        return register;
    }

    @Override
    public int monitorIdFor(Object object) {
        return monitorIds.computeIfAbsent(object, ignored -> monitorLockSeed.incrementAndGet());
    }

    @Override
    public int lockIdFor(Lock lock) {
        return lockIds.computeIfAbsent(lock, ignored -> monitorLockSeed.incrementAndGet());
    }

    @Override
    public Collection<? extends ThreadState> allActors() {
        return allActors;
    }

    @Override
    public RuntimeState advance(ThreadState selected, Duration waitTime, CheckpointRuntime checkpointRuntime) throws InterruptedException, TimeoutException {
        // we need to install and them uninstall ourselves as a callback
        checkpointRuntime.addCheckpointCallback(this);
        checkpointRuntime.removeCheckpointCallback(this);
        return null;
    }
}
