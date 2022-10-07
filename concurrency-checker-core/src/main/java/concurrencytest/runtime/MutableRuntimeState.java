package concurrencytest.runtime;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.description.LockAcquireCheckpointDescription;
import concurrencytest.checkpoint.description.LockReleaseCheckpointDescription;
import concurrencytest.checkpoint.description.MonitorCheckpointDescription;
import concurrencytest.runner.CheckpointReachedCallback;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.checkpoint.LockAcquireReleaseCheckpointReached;
import concurrencytest.runtime.checkpoint.MonitorCheckpointReached;
import concurrencytest.runtime.checkpoint.ThreadStartCheckpointReached;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MutableRuntimeState implements RuntimeState {

    private final CheckpointRegister register;
    private final StandardCheckpointRuntime checkpointRuntime;

    private final Map<Object, Integer> monitorIds;
    private final Map<Lock, Integer> lockIds;

    private final AtomicInteger monitorLockSeed;
    private final Map<String, ThreadState> allActors;
    private final Map<String, Consumer<Object>> threads;
    private final ThreadRendezvouCheckpointCallback rendezvouCallback;

    public MutableRuntimeState(CheckpointRegister register, Map<String, Consumer<Object>> managedThreadMap) {
        this.register = register;
        this.monitorIds = new ConcurrentHashMap<>();
        this.lockIds = new ConcurrentHashMap<>();
        this.monitorLockSeed = new AtomicInteger();
        this.allActors = managedThreadMap.keySet().stream().map(ThreadState::new).collect(Collectors.toMap(ThreadState::actorName, t -> t));
        this.threads = managedThreadMap;
        this.checkpointRuntime = new StandardCheckpointRuntime(register);
        this.rendezvouCallback = new ThreadRendezvouCheckpointCallback();
        this.checkpointRuntime.addCheckpointCallback(new CheckpointReachedCallback() {
            @Override
            public void checkpointReached(CheckpointReached checkpointReached) {
                if (checkpointReached instanceof MonitorCheckpointReached mon) {
                    registerMonitorCheckpoint(mon);
                } else if (checkpointReached instanceof LockAcquireReleaseCheckpointReached cp) {
                    registerLockAcquireRelease(cp);
                }
            }

        });
        this.checkpointRuntime.addCheckpointCallback(new CheckpointReachedCallback() {
            @Override
            public void checkpointReached(CheckpointReached checkpointReached) {
                if (checkpointReached instanceof ThreadStartCheckpointReached cp) {
                    registerNewActor(cp);
                }
            }
        });
        this.checkpointRuntime.addCheckpointCallback(rendezvouCallback);
    }

    private void registerLockAcquireRelease(LockAcquireReleaseCheckpointReached checkpointReached) {
        int lockId = lockIdFor(checkpointReached.theLock());
        String actorName = checkpointReached.actorName();
        ThreadState state = Objects.requireNonNull(allActors.remove(actorName), "actor with name %s not found".formatted(actorName));
        if (checkpointReached.checkpoint().description() instanceof LockAcquireCheckpointDescription lacq) {
            if (lacq.injectionPoint() == InjectionPoint.BEFORE) {
                allActors.put(actorName, state.beforeLockAcquisition(lockId, lacq));
            } else {
                allActors.put(actorName, state.lockAcquired(lockId));
            }
        } else if (checkpointReached.checkpoint().description() instanceof LockReleaseCheckpointDescription lr && lr.injectionPoint() == InjectionPoint.AFTER) {
            allActors.put(actorName, state.lockReleased(lockId));
        }
    }

    private void registerNewActor(ThreadStartCheckpointReached checkpointReached) {
        String brandNewActor = checkpointReached.newActorName();
        ThreadState old = allActors.putIfAbsent(brandNewActor, new ThreadState(brandNewActor));
        if (old != null) {
            throw new IllegalArgumentException("actor named %s was already registered? ".formatted(brandNewActor));
        }
    }

    private void registerMonitorCheckpoint(MonitorCheckpointReached mon) {
        int monitorId = monitorIdFor(mon.monitorOwner());
        String actorName = mon.actorName();
        ThreadState state = Objects.requireNonNull(allActors.remove(actorName), "actor with name %s not found".formatted(actorName));
        MonitorCheckpointDescription description = (MonitorCheckpointDescription) mon.checkpoint().description();
        if (description.monitorAcquire()) {
            if (mon.checkpoint().injectionPoint() == InjectionPoint.BEFORE) {
                allActors.put(actorName, state.beforeMonitorAcquire(monitorId, description));
            } else {
                allActors.put(actorName, state.monitorAcquired(monitorId));
            }
        } else if (mon.checkpoint().injectionPoint() == InjectionPoint.AFTER) {
            allActors.put(actorName, state.monitorReleased(monitorId));
        }
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
    public Collection<ManagedThread> start(Object testInstance, Duration timeout) throws InterruptedException, TimeoutException {
        Collection<ManagedThread> list = new ArrayList<>(this.threads.size());
        Set<String> actorsStarted = new HashSet<>();
        threads.forEach((actor, task) -> {
            ManagedThread m = new ManagedThread(() -> task.accept(testInstance), checkpointRuntime, actor);
            m.start();
            list.add(m);
            actorsStarted.add(actor);
        });
        rendezvouCallback.waitForActors(timeout, actorsStarted);
        return list;
    }

    @Override
    public RuntimeState advance(ThreadState selected, Duration maxWaitTime) throws InterruptedException, TimeoutException {
        Set<String> before = new HashSet<>(this.allActors.keySet());
        rendezvouCallback.resumeActor(selected.actorName());
        rendezvouCallback.waitForActors(maxWaitTime, before);
        CheckpointReached lastCheckpoint = rendezvouCallback.lastKnownCheckpoint(selected.actorName());
        if (lastCheckpoint instanceof ThreadStartCheckpointReached ts) {
            before.add(ts.newActorName());
            rendezvouCallback.waitForActors(maxWaitTime, before);
        }
        ThreadState newActorState = Objects.requireNonNull(allActors.remove(selected.actorName()), "actor state for %s not found".formatted(selected.actorName())).newCheckpointReached(lastCheckpoint, register.isFinishedCheckpoint(lastCheckpoint.checkpointId()));
        if (!newActorState.finished()) {
            allActors.put(selected.actorName(), newActorState);
        }
        return this;
    }
}
