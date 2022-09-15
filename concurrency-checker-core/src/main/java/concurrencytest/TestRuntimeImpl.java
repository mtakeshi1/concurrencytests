package concurrencytest;

import concurrencytest.checkpoint.OldCheckpointImpl;
import concurrencytest.util.InstrospectionHelper;
import concurrencytest.util.Murmur3A;
import concurrencytest.util.ReflectionHelper;
import org.objectweb.asm.Type;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class TestRuntimeImpl {

    public static final int DEFAULT_ACTOR_TIMEOUT_SECONDS = 10;
    public static final int DEFAULT_MAX_LOOP_COUNT = 1000;
    private static final ThreadLocal<TestRuntimeImpl> CURRENT_RUNTIME = new ThreadLocal<>();

    private final TestActor[] tasks;
    private final ExecutionGraph graph;
    private final ThreadGroup group;
    private final int maxLoopCount;
    private final int actorTimeoutSeconds;
    private final ClassLoader classLoader;
    private final boolean randomPick;
    private volatile boolean disabled;
    private final Runnable invariantsCheck;

    private final Collection<ManagedThread> managedThreads = new HashSet<>();

    private final OldCheckpointImpl threadFinishedCheckpoint = new OldCheckpointImpl(-3, "thread-finished");

    private final LockMonitorObserver monitorObserver = new LockMonitorObserver("monitor");
    private final LockMonitorObserver lockObserver = new LockMonitorObserver("lock");

    public TestRuntimeImpl(TestActor[] tasks, ExecutionGraph graph, Runnable invariantsCheck, int runNumber, int maxLoopCount, int actorTimeoutSeconds, boolean randomPick, ClassLoader loader) {
        this.tasks = tasks;
        this.graph = graph;
        this.group = new ThreadGroup("test-actor-group-#" + runNumber);
        this.invariantsCheck = invariantsCheck;
        this.maxLoopCount = maxLoopCount;
        this.actorTimeoutSeconds = actorTimeoutSeconds;
        this.classLoader = loader;
        this.randomPick = randomPick;
    }

    public void dumpThreads() {
        for (ManagedThread thread : this.managedThreads) {
            System.err.println("Stack trace for thread: " + thread);
            for (var element : thread.getStackTrace()) {
                System.err.println(element);
            }
            System.err.println("--------------------------------------------------");
        }
    }

    public static void setCurrentInstance(TestRuntimeImpl testRuntime) {
        CURRENT_RUNTIME.set(testRuntime);
    }

    public ExecutionDescription execute(String firstNode) throws InterruptedException {
        List<Map.Entry<String, OldCheckpointImpl>> pathTaken = new ArrayList<>();
        List<String> path = new ArrayList<>();
        startManagedThreads();
        try {
            rendezvous();
            Node lastNode = null;
            while (runningThreads() > 0) {
                invariantsCheck.run();
                RuntimeState currentThreadStates = currentRuntimeState();
                if (lastNode == null) {
                    lastNode = graph.reachedState(path, currentThreadStates);
                } else {
                    lastNode = lastNode.linkTo(path.get(path.size() - 1), new Node(currentThreadStates));
                }
                Collection<ManagedThread> alive = managedThreads.stream().filter(Thread::isAlive).collect(Collectors.toList());
                if (alive.isEmpty()) {
                    graph.markTerminal(currentThreadStates);
                    invariantsCheck.run();
                    return ExecutionDescription.executionCompleted(pathTaken);
                }

                Map<String, ManagedThread> canAdvance = new HashMap<>();

                alive.stream().filter(mt -> toThreadState(mt).isRunnable()).forEach(mt -> canAdvance.put(mt.getName(), mt));
                if (canAdvance.isEmpty()) {
                    return ExecutionDescription.deadlockFound(alive, pathTaken);
                }
                String nextThread;
                if (firstNode == null) {
                    nextThread = lastNode.findNextThread(canAdvance.keySet(), this.randomPick);
                } else {
                    nextThread = firstNode;
                    firstNode = null;
                }
                ManagedThread managedThread = canAdvance.get(nextThread);
                if (managedThread == null) {
                    lastNode.markTerminal();
                    return null;
//                    throw new RuntimeException("??");
                }
                if (managedThread.getLoopCount() > maxLoopCount) {
                    throw new RuntimeException("MAX LOOP COUNT reached ( " + maxLoopCount + " ) ");
                }
                monitorObserver.signalMonitorAcquiredIfNecessary(managedThread);
                lockObserver.signalMonitorAcquiredIfNecessary(managedThread);
                managedThread.resumeCheckpoint();
                rendezvous();
                if (managedThread.getCheckpoint() == null) {
                    if (managedThread.isAlive()) {
                        throw new RuntimeException("thread " + managedThread.getName() + " didn't reach a checkpoint but was still alive ");
                    } else {
                        checkLocksOwnedBy(managedThread);
                        checkMonitorsOwnedBy(managedThread);
                        pathTaken.add(Map.entry(managedThread.getName(), threadFinishedCheckpoint));
                        path.add(managedThread.getName());
                    }
                } else {
                    pathTaken.add(Map.entry(managedThread.getName(), managedThread.getCheckpoint()));
                    path.add(managedThread.getName());
                }
            }
            graph.reachedState(path, currentRuntimeState()).markTerminal();
            return ExecutionDescription.executionCompleted(pathTaken);
        } catch (InterruptedException e) {
            System.err.println("Interrupted path: ");
            for (var pathEntry : pathTaken) {
                System.err.println("thread: " + pathEntry.getKey() + " \t " + pathEntry.getValue());
            }
            System.err.println("-----------------------------------------------------");
            dumpThreads();
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            while (group.activeCount() > 0) {
                dettachThreads();
                Thread.sleep(100);
            }
        }
    }

    public ExecutionDescription resumeFrom(Queue<String> pathPreffix) throws InterruptedException {
        List<Map.Entry<String, OldCheckpointImpl>> pathTaken = new ArrayList<>();
        List<String> path = new ArrayList<>();
        startManagedThreads();
        try {
            rendezvous();
            Node lastNode = null;
            while (runningThreads() > 0) {
                invariantsCheck.run();
                RuntimeState currentThreadStates = currentRuntimeState();
                if (lastNode == null) {
                    lastNode = graph.reachedState(path, currentThreadStates);
                } else {
                    lastNode = lastNode.linkTo(path.get(path.size() - 1), new Node(currentThreadStates));
                }
                Collection<ManagedThread> alive = managedThreads.stream().filter(Thread::isAlive).collect(Collectors.toList());
                if (alive.isEmpty()) {
                    graph.markTerminal(currentThreadStates);
                    invariantsCheck.run();
                    return ExecutionDescription.executionCompleted(pathTaken);
                }

                Map<String, ManagedThread> canAdvance = new HashMap<>();

                alive.stream().filter(mt -> toThreadState(mt).isRunnable()).forEach(mt -> canAdvance.put(mt.getName(), mt));
                if (canAdvance.isEmpty()) {
                    return ExecutionDescription.deadlockFound(alive, pathTaken);
                }
                String nextThread;
                if (pathPreffix == null || pathPreffix.isEmpty()) {
                    nextThread = lastNode.findNextThread(canAdvance.keySet(), this.randomPick);
                } else {
                    nextThread = pathPreffix.poll();
                }
                ManagedThread managedThread = canAdvance.get(nextThread);
                if (managedThread == null) {
                    lastNode.markTerminal();
                    return null;
//                    throw new RuntimeException("??");
                }
                if (managedThread.getLoopCount() > maxLoopCount) {
                    throw new RuntimeException("MAX LOOP COUNT reached ( " + maxLoopCount + " ) ");
                }
                monitorObserver.signalMonitorAcquiredIfNecessary(managedThread);
                lockObserver.signalMonitorAcquiredIfNecessary(managedThread);
                managedThread.resumeCheckpoint();
                rendezvous();
                if (managedThread.getCheckpoint() == null) {
                    if (managedThread.isAlive()) {
                        throw new RuntimeException("thread " + managedThread.getName() + " didn't reach a checkpoint but was still alive ");
                    } else {
                        checkLocksOwnedBy(managedThread);
                        checkMonitorsOwnedBy(managedThread);
                        pathTaken.add(Map.entry(managedThread.getName(), threadFinishedCheckpoint));
                        path.add(managedThread.getName());
                    }
                } else {
                    pathTaken.add(Map.entry(managedThread.getName(), managedThread.getCheckpoint()));
                    path.add(managedThread.getName());
                }
            }
            graph.reachedState(path, currentRuntimeState()).markTerminal();
            return ExecutionDescription.executionCompleted(pathTaken);
        } catch (InterruptedException e) {
            System.err.println("Interrupted path: ");
            for (var pathEntry : pathTaken) {
                System.err.println("thread: " + pathEntry.getKey() + " \t " + pathEntry.getValue());
            }
            System.err.println("-----------------------------------------------------");
            dumpThreads();
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            while (group.activeCount() > 0) {
                dettachThreads();
                Thread.sleep(100);
            }
        }
    }

    public ExecutionDescription executeFork(ThreadPoolExecutor executor) throws InterruptedException {
        List<Map.Entry<String, OldCheckpointImpl>> pathTaken = new ArrayList<>();
        List<String> path = new ArrayList<>();
        startManagedThreads();
        try {
            rendezvous();
            Node lastNode = null;
            while (runningThreads() > 0) {
                invariantsCheck.run();
                RuntimeState currentThreadStates = currentRuntimeState();
                if (lastNode == null) {
                    lastNode = graph.reachedState(path, currentThreadStates);
                } else {
                    lastNode = lastNode.linkTo(path.get(path.size() - 1), new Node(currentThreadStates));
                }
                Collection<ManagedThread> alive = managedThreads.stream().filter(Thread::isAlive).collect(Collectors.toList());
                if (alive.isEmpty()) {
                    graph.markTerminal(currentThreadStates);
                    invariantsCheck.run();
                    return ExecutionDescription.executionCompleted(pathTaken);
                }

                Map<String, ManagedThread> canAdvance = new HashMap<>();

                alive.stream().filter(mt -> toThreadState(mt).isRunnable()).forEach(mt -> canAdvance.put(mt.getName(), mt));
                if (canAdvance.isEmpty()) {
                    return ExecutionDescription.deadlockFound(alive, pathTaken);
                }
                String nextThread = lastNode.findNextThread(canAdvance.keySet(), this.randomPick);
                ManagedThread managedThread = canAdvance.get(nextThread);
                if (managedThread == null) {
                    lastNode.markTerminal();
                    return null;
//                    throw new RuntimeException("??");
                }
                if (managedThread.getLoopCount() > maxLoopCount) {
                    throw new RuntimeException("MAX LOOP COUNT reached ( " + maxLoopCount + " ) ");
                }
                monitorObserver.signalMonitorAcquiredIfNecessary(managedThread);
                lockObserver.signalMonitorAcquiredIfNecessary(managedThread);
                managedThread.resumeCheckpoint();
                rendezvous();
                if (managedThread.getCheckpoint() == null) {
                    if (managedThread.isAlive()) {
                        throw new RuntimeException("thread " + managedThread.getName() + " didn't reach a checkpoint but was still alive ");
                    } else {
                        checkLocksOwnedBy(managedThread);
                        checkMonitorsOwnedBy(managedThread);
                        pathTaken.add(Map.entry(managedThread.getName(), threadFinishedCheckpoint));
                        path.add(managedThread.getName());
                    }
                } else {
                    pathTaken.add(Map.entry(managedThread.getName(), managedThread.getCheckpoint()));
                    path.add(managedThread.getName());
                }
            }
            graph.reachedState(path, currentRuntimeState()).markTerminal();
            return ExecutionDescription.executionCompleted(pathTaken);
        } catch (InterruptedException e) {
            System.err.println("Interrupted path: ");
            for (var pathEntry : pathTaken) {
                System.err.println("thread: " + pathEntry.getKey() + " \t " + pathEntry.getValue());
            }
            System.err.println("-----------------------------------------------------");
            dumpThreads();
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            while (group.activeCount() > 0) {
                dettachThreads();
                Thread.sleep(100);
            }
        }
    }

    public ExecutionDescription execute() throws InterruptedException {
        return this.execute(null);
    }

    private void checkMonitorsOwnedBy(ManagedThread managedThread) {
        monitorObserver.checkThreadFinished(managedThread);
    }

    private void checkLocksOwnedBy(ManagedThread managedThread) {
        lockObserver.checkThreadFinished(managedThread);
    }

    private void dettachThreads() {
        this.disabled = true;
        for (var mt : managedThreads) {
            synchronized (mt) {
                mt.notifyAll();
            }
        }
    }

    private synchronized RuntimeState currentRuntimeState() {
        Map<String, ThreadState> threadStates = new HashMap<>();
        managedThreads.forEach(mt -> threadStates.put(mt.getName(), toThreadState(mt)));
        return new RuntimeState(threadStates);
    }

    private ThreadState toThreadState(ManagedThread mt) {
        String blockedInfo;
        if ((blockedInfo = lockObserver.blockedInformation(mt)) != null || ((blockedInfo = monitorObserver.blockedInformation(mt)) != null)) {
            return new ThreadState(mt.getName(), mt.getCheckpoint(), mt.isAlive(), false, blockedInfo, mt.getLoopCount());
        } else {
            return new ThreadState(mt.getName(), mt.getCheckpoint(), mt.isAlive(), mt.canAdvance(), mt.getLoopCount());
        }
    }

    private void startManagedThreads() {
        for (TestActor actor : this.tasks) {
            ManagedThread thread = new ManagedThread(this.group, actor.getTask(), actor.getName(), this);
            if (this.classLoader != null) {
                thread.setContextClassLoader(this.classLoader);
            }
            thread.start();
            managedThreads.add(thread);
        }
    }

    private void rendezvous() throws InterruptedException {
        for (ManagedThread thread : this.managedThreads) {
            thread.waitForCheckpoint(actorTimeoutSeconds);
            if (thread.isAlive() && thread.getCheckpoint() == null) {
                //
                checkForDeadlock();
                throw new RuntimeException("thread " + thread.getName() + " did not reach a checkpoint within: " + actorTimeoutSeconds + "s");
            }
        }
    }

    public static void checkForDeadlock() {
        long[] deadlockedThreads = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        if (deadlockedThreads != null) {
            String[] threadNames = threadNamesForIds(deadlockedThreads);
            throw new RuntimeException("Deadlock reached for locks between threads named: " + Arrays.toString(threadNames));
        }
        long[] threads = ManagementFactory.getThreadMXBean().findMonitorDeadlockedThreads();
        if (threads != null) {
            throw new RuntimeException("Deadlock reached for monitors between threads named: " + Arrays.toString(threadNamesForIds(threads)));
        }
    }

    public static String[] threadNamesForIds(long[] deadlockedThreads) {
        String[] threadNames = new String[deadlockedThreads.length];
        ThreadInfo[] infos = ManagementFactory.getThreadMXBean().getThreadInfo(deadlockedThreads);
        for (int i = 0; i < deadlockedThreads.length; i++) {
            threadNames[i] = infos[i].getThreadName();
        }
        return threadNames;
    }


    private int runningThreads() {
        return (int) managedThreads.stream().filter(Thread::isAlive).count();
    }

    public static void autoCheckpoint(String name, Object _this) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return;
        }
        Throwable t = new Throwable().fillInStackTrace();
        var sb = new StringBuilder();
        for (int i = 1; i < t.getStackTrace().length; i++) {
            sb.append(t.getStackTrace()[i].toString()).append("\n");
        }
        OldCheckpointImpl c = testRuntime.getOrCreateCheckpoint(name, sb.toString(), Map.of("this", _this));
        checkpointReached(c.checkpointId());
    }

    public boolean isDisabled() {
        return disabled;
    }

    public static void autoCheckpoint(Object _this) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return;
        }
        autoCheckpoint(InstrospectionHelper.findRelevantStackInfo(), _this);
    }

    public static boolean checkActualDispatchForMonitor(Object callTarget, String methodName, String methodDescription, long checkpointId, String checkpointName, String description) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return false;
        }
        Type methodType = Type.getMethodType(methodDescription);
        Class[] declaredParameterTypes = toClassArray(methodType.getArgumentTypes());
        Method m = tryFindMethod(callTarget.getClass(), methodName, declaredParameterTypes);
        if (Modifier.isSynchronized(m.getModifiers())) {
            beforeMonitorAcquiredCheckpoint(callTarget, checkpointId, checkpointName, "synchronized " + description);
            return true;
        }
        return false;
    }

    public static boolean checkActualDispatchForStaticMethod(Class<?> callTarget, String methodName, String methodDescription, long checkpointId, String checkpointName, String description) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return false;
        }
        Type methodType = Type.getMethodType(methodDescription);
        Class[] declaredParameterTypes = toClassArray(methodType.getArgumentTypes());
        Method m = tryFindMethod(callTarget, methodName, declaredParameterTypes);
        if (Modifier.isSynchronized(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
            beforeMonitorAcquiredCheckpoint(callTarget, checkpointId, checkpointName, description);
            return true;
        }
        return false;
    }

    private static Method tryFindMethod(Class<?> aClass, String methodName, Class[] declaredParameterTypes) {
        if (aClass == null) {
            return null;
        }
        try {
            return aClass.getDeclaredMethod(methodName, declaredParameterTypes);
        } catch (NoSuchMethodException e) {
        }
        for (Class<?> iface : aClass.getInterfaces()) {
            try {
                return iface.getDeclaredMethod(methodName, declaredParameterTypes);
            } catch (NoSuchMethodException e) {
            }
        }
        return tryFindMethod(aClass.getSuperclass(), methodName, declaredParameterTypes);
    }

    private static Class[] toClassArray(Type[] argumentTypes) {
        Class[] array = new Class[argumentTypes.length];
        for (int i = 0; i < argumentTypes.length; i++) {
            try {
                array[i] = ReflectionHelper.resolveType(argumentTypes[i].getClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not resolve type with name: " + argumentTypes[i].getClassName() + " - internal name: " + argumentTypes[i].getInternalName());
            }
        }
        return array;
    }

    public static void beforeMonitorAcquiredCheckpoint(Object monitor, long id, String name, String description) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return;
        }
        ManagedThread managedThread = (ManagedThread) Thread.currentThread();
        OldCheckpointImpl checkpoint = testRuntime.graph.computeCheckpointIfAbsent(id, i -> new OldCheckpointImpl(i, name, description, Collections.emptyMap()));
        testRuntime.monitorObserver.waitingForMonitor(managedThread, monitor, description);
        managedThread.setCheckpointAndWait(checkpoint);
    }

    public static void afterMonitorReleasedCheckpoint(Object monitor, long id, String name, String description) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return;
        }
        ManagedThread thread = (ManagedThread) Thread.currentThread();
        OldCheckpointImpl checkpoint = testRuntime.graph.computeCheckpointIfAbsent(id, i -> new OldCheckpointImpl(i, name, description, Collections.emptyMap()));
        testRuntime.monitorObserver.monitorReleased(thread, monitor);
        thread.setCheckpointAndWait(checkpoint);
    }

    public static void beforeLockAcquiredCheckpoint(Lock lock, long id, String name, String description) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return;
        }
        ManagedThread managedThread = (ManagedThread) Thread.currentThread();
        OldCheckpointImpl checkpoint = testRuntime.graph.computeCheckpointIfAbsent(id, i -> new OldCheckpointImpl(i, name, description, Collections.emptyMap()));
        testRuntime.lockObserver.waitingForMonitor(managedThread, lock, description);
        managedThread.setCheckpointAndWait(checkpoint);
    }

    public static void afterLockReleasedCheckpoint(Lock lock, long id, String name, String description) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return;
        }
        ManagedThread thread = (ManagedThread) Thread.currentThread();
        OldCheckpointImpl checkpoint = testRuntime.graph.computeCheckpointIfAbsent(id, i -> new OldCheckpointImpl(i, name, description, Collections.emptyMap()));
        testRuntime.lockObserver.monitorReleased(thread, lock);
        thread.setCheckpointAndWait(checkpoint);
    }

    private OldCheckpointImpl getOrCreateCheckpoint(String name, String description, Map<String, Object> context) {
        Murmur3A murmur3A = new Murmur3A(description.length());
        murmur3A.update(description.getBytes(StandardCharsets.UTF_8));
        murmur3A.update(name.getBytes(StandardCharsets.UTF_8));
        long value = murmur3A.getValue();
        return graph.computeCheckpointIfAbsent(value, v -> new OldCheckpointImpl(v, name, description, context));
    }


    public static void checkpointReached(long id) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return;
        }
        ManagedThread thread = (ManagedThread) Thread.currentThread();
        OldCheckpointImpl checkpoint = testRuntime.graph.getExistingCheckpont(id);
        if (checkpoint == null) {
            throw new RuntimeException("Unknown checkpoint with id: " + id);
        }
        thread.setCheckpointAndWait(checkpoint);
    }


    public static void checkpointReached(long id, String name, String description) {
        TestRuntimeImpl testRuntime = CURRENT_RUNTIME.get();
        if (testRuntime == null) {
            return;
        }
        ManagedThread thread = (ManagedThread) Thread.currentThread();
        OldCheckpointImpl checkpoint = testRuntime.graph.computeCheckpointIfAbsent(id, i -> new OldCheckpointImpl(i, name, description, Collections.emptyMap()));
        thread.setCheckpointAndWait(checkpoint);
    }

}
