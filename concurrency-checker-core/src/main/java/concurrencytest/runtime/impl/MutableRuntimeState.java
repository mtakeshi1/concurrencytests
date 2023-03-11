package concurrencytest.runtime.impl;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.checkpoint.description.LockAcquireCheckpointDescription;
import concurrencytest.checkpoint.description.LockReleaseCheckpointDescription;
import concurrencytest.checkpoint.description.MonitorCheckpointDescription;
import concurrencytest.checkpoint.instance.*;
import concurrencytest.config.Configuration;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.StandardCheckpointRuntime;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.lock.LockType;
import concurrencytest.runtime.thread.ManagedThread;
import concurrencytest.runtime.thread.RunnableThreadState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.runtime.thread.WaitingAwaitingThreadState;
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

    private final List<ExecutionPath> executionPath = new ArrayList<>();
    private final Consumer<Throwable> errorReporter;
    private final ExecutorService executorService;
    private final Configuration configuration;

    record WaitCountKey(LockType lockType, String actorName, int resourceId, String sourceCode, int lineNumber) {
    }

    record LockOrMonitor(LockType lockType, int resourceId) {
    }

    private final ConcurrentMap<WaitCountKey, Integer> waitCount = new ConcurrentHashMap<>();

    private final ConcurrentMap<LockOrMonitor, Integer> nofityCount = new ConcurrentHashMap<>();

//    private final Map<>

    public MutableRuntimeState(CheckpointRegister register, Map<String, Function<Object, Throwable>> managedThreadMap, Consumer<Throwable> errorReporter, ExecutorService executorService, Configuration configuration) {
        this.configuration = configuration;
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
                    allActors.putIfAbsent(mt.getActorName(), new RunnableThreadState(mt.getActorName(), cb.checkpointId()));
                }
            }
        });

        checkpointRuntime.addTypedCheckpointCallback(WaitCheckpointReached.class, this::threadWaitCheckpointReached);
        checkpointRuntime.addTypedCheckpointCallback(MonitorCheckpointReached.class, this::registerMonitorCheckpoint);
        checkpointRuntime.addTypedCheckpointCallback(LockCheckpointReached.class, this::registerLockAcquireRelease);

        checkpointRuntime.addCheckpointCallback(checkpointReached -> {
            synchronized (this) {
                executionPath.add(new ExecutionPath(checkpointReached.actorName(), checkpointReached.checkpointId(), checkpointReached.details()));
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("reached checkpoint %d - %s - %s".formatted(checkpointReached.checkpointId(), checkpointReached.checkpoint().description(), checkpointReached.details()));
                }
            }
        });
        this.checkpointRuntime.addCheckpointCallback(rendezvouCallback);
    }

    public List<ExecutionPath> getExecutionPath() {
        return Collections.unmodifiableList(executionPath);
    }

    @Override
    public Optional<Throwable> errorReported() {
        return this.checkpointRuntime.errorReported();
    }


    @Override
    public int getWaitCount(ThreadState actor, CheckpointDescription acquisitionPoint, int resourceId, LockType lockType) {
        WaitCountKey key = new WaitCountKey(lockType, actor.actorName(), resourceId, acquisitionPoint.sourceFile(), acquisitionPoint.lineNumber());
        return waitCount.getOrDefault(key, 0);
    }

    @Override
    public boolean isNotifySignalAvailable(int resourceId, boolean monitor) {
        return nofityCount.getOrDefault(new LockOrMonitor(monitor ? LockType.MONITOR : LockType.LOCK, resourceId), 0) > 0;
    }

    @Override
    public void addNotifySignal(int resourceId, boolean monitor) {
        LockOrMonitor key = new LockOrMonitor(monitor ? LockType.MONITOR : LockType.LOCK, resourceId);
        nofityCount.merge(key, 1, Integer::sum);
    }

    @Override
    public void consumeNotifySignal(int resourceId, boolean monitor) {
        LockOrMonitor key = new LockOrMonitor(monitor ? LockType.MONITOR : LockType.LOCK, resourceId);
        Integer remaining = nofityCount.merge(key, -1, Integer::sum);
        if (remaining < 0) {
            throw new IllegalStateException("Consumed more signals than what was available. Resource key: %s, remaining notify signals: %d".formatted(key, remaining));
        } else if (remaining == 0) {
            nofityCount.remove(key, remaining);
        }
    }

    @Override
    public Configuration configuration() {
        return this.configuration;
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

    private void threadWaitCheckpointReached(WaitCheckpointReached checkpointReached) {
        CheckpointDescription acquisitionPoint = checkpointReached.checkpoint().description();
        LockType type = checkpointReached.monitorWait() ? LockType.MONITOR : LockType.LOCK;
        int resourceId = type == LockType.MONITOR ? monitorIdFor(checkpointReached.monitorOrLock()) : lockIdFor((Lock) checkpointReached.monitorOrLock());
        String actorName = checkpointReached.actorName();
        ThreadState state = removeThreadStateForUpdate(actorName);
        if (acquisitionPoint.injectionPoint() == InjectionPoint.BEFORE) {
            WaitCountKey key = new WaitCountKey(type, actorName, resourceId, acquisitionPoint.sourceFile(), acquisitionPoint.lineNumber());
            waitCount.merge(key, 1, Integer::sum);
            // we must release the actor temporatily
            if (checkpointReached.monitorWait()) {
                ThreadState intermediary = state.monitorReleased(resourceId);
                allActors.put(actorName, new WaitingAwaitingThreadState(intermediary.actorName(), checkpointReached.checkpointId(), 0, intermediary.ownedResources(), acquisitionPoint,
                        new BlockingResource(LockType.MONITOR, resourceId, checkpointReached.monitorOrLock().getClass(), acquisitionPoint.sourceFile(), acquisitionPoint.lineNumber())));
            } else {
                ThreadState intermediary = state.lockReleased(resourceId);
                allActors.put(actorName, new WaitingAwaitingThreadState(intermediary.actorName(), checkpointReached.checkpointId(), 0, intermediary.ownedResources(), acquisitionPoint,
                        new BlockingResource(LockType.LOCK, resourceId, checkpointReached.monitorOrLock().getClass(), acquisitionPoint.sourceFile(), acquisitionPoint.lineNumber())));
            }
        } else {
            if (checkpointReached.monitorWait()) {
                ThreadState value = state
                        .newCheckpointReached(checkpointReached) // we reset state
                        .beforeMonitorAcquire(resourceId, checkpointReached.monitorOrLock(), checkpointReached.checkpoint().description())
                        .monitorAcquired(resourceId, checkpointReached.checkpoint().sourceFile(), checkpointReached.checkpoint().lineNumber());
                allActors.put(actorName, value);
            } else {

                ThreadState value = state
                        .newCheckpointReached(checkpointReached) // we reset state
                        .beforeLockAcquisition(resourceId, (Lock) checkpointReached.monitorOrLock(), checkpointReached.checkpoint().description())
                        .lockTryAcquire(resourceId, true, checkpointReached.checkpoint().sourceFile(), checkpointReached.checkpoint().lineNumber());
                allActors.put(actorName, value);
            }
        }
    }

    private ThreadState removeThreadStateForUpdate(String actorName) {
        return Objects.requireNonNull(allActors.remove(actorName), "actor state for name '%s' not found".formatted(actorName));
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
        //TODO not sure if we need to reset wait count
        ThreadState oldActorState = removeThreadStateForUpdate(selected.actorName());
        boolean finishedCheckpoint = register.isFinishedCheckpoint(lastCheckpoint.checkpointId());
        ThreadState newActorState = finishedCheckpoint ? oldActorState.actorFinished(lastCheckpoint) : oldActorState.newCheckpointReached(lastCheckpoint);
        allActors.put(selected.actorName(), newActorState);
        if (newActorState.finished()) {
            rendezvouCallback.actorFinished(selected.actorName(), maxWaitTime);
        }
        return this;
    }
}
