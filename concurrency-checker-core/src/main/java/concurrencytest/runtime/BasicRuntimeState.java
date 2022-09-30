package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.checkpoint.description.LockAcquireCheckpointDescription;
import concurrencytest.runner.CheckpointReachedCallback;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.checkpoint.LockAcquireReleaseCheckpoint;
import concurrencytest.runtime.checkpoint.MonitorCheckpointReached;
import concurrencytest.runtime.checkpoint.ThreadStartCheckpointReached;
import concurrencytest.runtime.tree.ThreadState;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class BasicRuntimeState implements RuntimeState {

    private final CheckpointRegister register;

    private final Map<Object, Integer> monitorIds = new ConcurrentHashMap<>();
    private final Map<Lock, Integer> lockIds = new ConcurrentHashMap<>();

    private final AtomicInteger monitorLockSeed = new AtomicInteger();
    private final Map<String, ThreadState> allActors;
    private final RecordingCheckpointRuntime checkpointRuntime;
    private final Map<String, Runnable> threads;
    private final ThreadRendezvouCheckpointCallback rendezvouCallback;


    public BasicRuntimeState(CheckpointRegister register, Map<String, Runnable> managedThreadMap) {
        this.register = register;
        this.allActors = managedThreadMap.keySet().stream().map(ThreadState::new).collect(Collectors.toMap(ThreadState::actorName, t -> t));
        this.threads = managedThreadMap;
        this.checkpointRuntime = new RecordingCheckpointRuntime(register);
        this.rendezvouCallback = new ThreadRendezvouCheckpointCallback();
        this.checkpointRuntime.addCheckpointCallback(new CheckpointReachedCallback() {
            @Override
            public void checkpointReached(ManagedThread managedThread, CheckpointReached checkpointReached, RuntimeState currentState) throws Exception {
                if (checkpointReached instanceof MonitorCheckpointReached mon) {
                    registerMonitorCheckpoint(mon);
                } else if (checkpointReached instanceof LockAcquireReleaseCheckpoint cp) {
                    registerLockAcquireRelease(cp);
                }
            }

        });
        this.checkpointRuntime.addCheckpointCallback(new CheckpointReachedCallback() {
            @Override
            public void checkpointReached(ManagedThread managedThread, CheckpointReached checkpointReached, RuntimeState currentState) throws Exception {
                if (checkpointReached instanceof ThreadStartCheckpointReached cp) {
                    registerNewActor(cp);
                }
            }
        });
        this.checkpointRuntime.addCheckpointCallback(rendezvouCallback);
    }

    private void registerLockAcquireRelease(LockAcquireReleaseCheckpoint checkpointReached) {
        throw new RuntimeException("not yet implemented");
    }

    private void registerNewActor(ThreadStartCheckpointReached checkpointReached) {
        throw new RuntimeException("not yet implemented");
    }

    private void registerMonitorCheckpoint(MonitorCheckpointReached mon) {
        throw new RuntimeException("not yet implemented");
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
        return allActors.values();
    }

    @Override
    public Collection<ManagedThread> start() {
        Collection<ManagedThread> list = new ArrayList<>(this.threads.size());
        threads.forEach((actor, task) -> {
            ManagedThread m = new ManagedThread(task, checkpointRuntime, actor);
            m.start();
            list.add(m);
        });
        return list;
    }

    @Override
    public RuntimeState advance(ThreadState selected, Duration maxWaitTime) throws InterruptedException, TimeoutException {
        // we need to install and them uninstall ourselves as a callback
        throw new RuntimeException("not yet implemented");
    }
}
