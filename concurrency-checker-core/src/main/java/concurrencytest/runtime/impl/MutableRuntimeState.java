package concurrencytest.runtime.impl;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.description.LockAcquireCheckpointDescription;
import concurrencytest.checkpoint.description.LockCheckpointReached;
import concurrencytest.checkpoint.description.LockReleaseCheckpointDescription;
import concurrencytest.checkpoint.description.MonitorCheckpointDescription;
import concurrencytest.runner.CheckpointReachedCallback;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.StandardCheckpointRuntime;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.checkpoint.LockAcquireCheckpointReached;
import concurrencytest.runtime.checkpoint.MonitorCheckpointReached;
import concurrencytest.runtime.checkpoint.ThreadStartCheckpointReached;
import concurrencytest.runtime.thread.ManagedThread;
import concurrencytest.runtime.thread.ThreadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MutableRuntimeState implements RuntimeState {

    private static final Logger LOGGER = LoggerFactory.getLogger(MutableRuntimeState.class);

    private final CheckpointRegister register;
    private final StandardCheckpointRuntime checkpointRuntime;

    private final Map<Object, Integer> monitorIds;
    private final Map<Lock, Integer> lockIds;

    private final AtomicInteger monitorLockSeed;
    private final Map<String, ThreadState> allActors;
    private final Map<String, Function<Object, Throwable>> threads;
    private final ThreadRendezvouCheckpointCallback rendezvouCallback;

    private final List<String> executionPath = new ArrayList<>();
    private final Consumer<Throwable> errorReporter;
    private final ExecutorService executorService;

    public MutableRuntimeState(CheckpointRegister register, Map<String, Function<Object, Throwable>> managedThreadMap, Consumer<Throwable> errorReporter, ExecutorService executorService) {
        this.register = register;
        this.monitorIds = new ConcurrentHashMap<>();
        this.lockIds = new ConcurrentHashMap<>();
        this.monitorLockSeed = new AtomicInteger();
        this.allActors = new ConcurrentHashMap<>();
        this.threads = managedThreadMap;
        this.checkpointRuntime = new StandardCheckpointRuntime(register);
        this.rendezvouCallback = new ThreadRendezvouCheckpointCallback();
        this.errorReporter = errorReporter;
        this.executorService = executorService;
        this.checkpointRuntime.addCheckpointCallback(cb -> {
            if (cb.checkpointId() == register.taskStartingCheckpoint().checkpointId()) {
                if (cb.thread() instanceof ManagedThread mt) {
                    allActors.putIfAbsent(mt.getActorName(), new ThreadState(mt.getActorName(), cb.checkpointId()));
                }
            }
        });

        this.checkpointRuntime.addCheckpointCallback(new CheckpointReachedCallback() {
            @Override
            public void checkpointReached(CheckpointReached checkpointReached) {
                if (checkpointReached instanceof MonitorCheckpointReached mon) {
                    registerMonitorCheckpoint(mon);
                } else if (checkpointReached instanceof LockCheckpointReached lockCheckpoint) {
                    registerLockAcquireRelease(lockCheckpoint);
                }
            }

        });
        checkpointRuntime.addCheckpointCallback(checkpointReached -> {
            synchronized (this) {
                var state = Objects.requireNonNull(allActors.get(checkpointReached.actorName()), "state not found for actor named '%s'".formatted(checkpointReached.actorName()));
                executionPath.add("[%s] %s - current state: %s".formatted(checkpointReached.actorName(), checkpointReached.checkpoint().description(), state));
                if(LOGGER.isTraceEnabled()) {
                    LOGGER.trace("reached checkpoint %d - %s - %s".formatted(checkpointReached.checkpointId(), checkpointReached.checkpoint().description(), checkpointReached.details()));
                }
            }
        });
        this.checkpointRuntime.addCheckpointCallback(rendezvouCallback);
    }

    public List<String> getExecutionPath() {
        return Collections.unmodifiableList(executionPath);
    }

    @Override
    public Optional<Throwable> errorReported() {
        return this.checkpointRuntime.errorReported();
    }

    private void registerLockAcquireRelease(LockCheckpointReached checkpointReached) {
        int lockId = lockIdFor(checkpointReached.theLock());
        String actorName = checkpointReached.actorName();
        ThreadState state = removeThreadStateForUpdate(actorName);
        if (checkpointReached instanceof LockAcquireCheckpointReached reached && checkpointReached.checkpoint().description() instanceof LockAcquireCheckpointDescription lacq) {
            if (lacq.injectionPoint() == InjectionPoint.BEFORE) {
                allActors.put(actorName, state.beforeLockAcquisition(lockId, checkpointReached.theLock(), lacq));
            } else {
                allActors.put(actorName, state.lockTryAcquire(lockId, reached.result().acquired(), checkpointReached.checkpoint().sourceFile(), checkpointReached.checkpoint().lineNumber()));
            }
        } else if (checkpointReached.checkpoint().description() instanceof LockReleaseCheckpointDescription lr && lr.injectionPoint() == InjectionPoint.AFTER) {
            allActors.put(actorName, state.lockReleased(lockId));
        } else {
            allActors.put(actorName, state);
        }
    }

    private ThreadState removeThreadStateForUpdate(String actorName) {
        return Objects.requireNonNull(allActors.remove(actorName), "actor state for name '%s' not found".formatted(actorName));
    }

    private void registerMonitorCheckpoint(MonitorCheckpointReached mon) {
        int monitorId = monitorIdFor(mon.monitorOwner());
        String actorName = mon.actorName();
        ThreadState state = removeThreadStateForUpdate(actorName);
        MonitorCheckpointDescription description = (MonitorCheckpointDescription) mon.checkpoint().description();
        if (description.monitorAcquire()) {
            if (mon.checkpoint().injectionPoint() == InjectionPoint.BEFORE) {
                allActors.put(actorName, state.beforeMonitorAcquire(monitorId, mon.monitorOwner(), description));
            } else {
                allActors.put(actorName, state.monitorAcquired(monitorId, description.sourceFile(), description.lineNumber()));
            }
        } else if (mon.checkpoint().injectionPoint() == InjectionPoint.AFTER) {
            allActors.put(actorName, state.monitorReleased(monitorId));
        } else {
            allActors.put(actorName, state);
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
    public Collection<Future<Throwable>> start(Object testInstance, Duration timeout) throws InterruptedException, TimeoutException {
        Map<String, Future<Throwable>> actorTasks = new HashMap<>(this.threads.size());
        Set<String> actorsStarted = new HashSet<>();
        threads.forEach((actor, task) -> {
            Callable<Throwable> runnable = () -> {
                ManagedThread mt = (ManagedThread) Thread.currentThread();
                mt.setup(actor, checkpointRuntime);
                try {
                    Throwable t = task.apply(testInstance);
                    if (t != null) {
                        errorReporter.accept(t);
                    }
                    return t;
                } finally {
                    mt.cleanup();
                }
            };
            actorTasks.put(actor, executorService.submit(runnable));
            actorsStarted.add(actor);
        });
        rendezvouCallback.setActorTasks(actorTasks);
        rendezvouCallback.waitForActors(timeout, actorsStarted);
        return actorTasks.values();
    }

    @Override
    public RuntimeState advance(ThreadState selected, Duration maxWaitTime) throws InterruptedException, TimeoutException {
        Set<String> before = allActors.entrySet().stream().filter(e -> !e.getValue().finished()).map(Map.Entry::getKey).collect(Collectors.toSet());
        rendezvouCallback.resumeActor(selected.actorName());
        rendezvouCallback.waitForActors(maxWaitTime, before);
        CheckpointReached lastCheckpoint = rendezvouCallback.lastKnownCheckpoint(selected.actorName());
        if (lastCheckpoint instanceof ThreadStartCheckpointReached ts) {
            before.add(ts.newActorName());
            ManagedThread freshThread = ts.freshThread();
            AtomicReference<Throwable> errorReporter = new AtomicReference<>();
            freshThread.setUncaughtExceptionHandler((t, e) -> errorReporter.set(e));
            Future<Throwable> taskMonitor = new Future<>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    if (mayInterruptIfRunning) {
                        freshThread.interrupt();
                    }
                    return !freshThread.isAlive();
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return errorReporter.get() != null || !freshThread.isAlive();
                }

                @Override
                public Throwable get() throws InterruptedException {
                    freshThread.join();
                    return errorReporter.get();
                }

                @Override
                public Throwable get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
                    freshThread.join(unit.toMillis(timeout));
                    if (freshThread.isAlive()) {
                        throw new TimeoutException();
                    }
                    return errorReporter.get();
                }
            };
            rendezvouCallback.registerNewTask(ts.newActorName(), taskMonitor);
//            ts.freshThread().setUncaughtExceptionHandler();
            rendezvouCallback.waitForActors(maxWaitTime, before);
        }
        ThreadState oldActorState = removeThreadStateForUpdate(selected.actorName());
        ThreadState newActorState = oldActorState.newCheckpointReached(lastCheckpoint, register.isFinishedCheckpoint(lastCheckpoint.checkpointId()));
        allActors.put(selected.actorName(), newActorState);
        if (newActorState.finished()) {
            rendezvouCallback.actorFinished(selected.actorName(), maxWaitTime);
        }
        return this;
    }
}
