package concurrencytest;

import concurrencytest.agent.Agent;
import concurrencytest.agent.InjectCheckpointVisitor;
import concurrencytest.agent.OpenClassLoader;
import concurrencytest.agent.ReadClassesVisitor;
import concurrencytest.annotations.*;
import concurrencytest.checkpoint.OldCheckpointImpl;
import concurrencytest.util.ASMUtils;
import concurrencytest.util.LongStatistics;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ConcurrencyRunner extends Runner {

    public static final int DEFAULT_RUN_TIMEOUT_SECONDS = 10;
    private final Class<?> testClass;
    private final String testName;

    private static final AtomicLong runnerGeneratedCheckpointIds = new AtomicLong(-3);

    private OpenClassLoader loader;

    public static volatile boolean agentDone;

    public static volatile Throwable agentError;

    public static synchronized void agentDone(Throwable error) {
        agentError = error;
        agentDone = true;
        ConcurrencyRunner.class.notifyAll();
    }

    public ConcurrencyRunner(Class<?> testClass) {
        this.testClass = testClass;
        this.testName = Arrays.stream(testClass.getMethods()).filter(m -> m.isAnnotationPresent(Actor.class)).map(Method::getName).collect(Collectors.joining("_"));
    }

    public static TestParameters getParameterAnnotation(Class<?> testClass) {
        TestParameters annotation = testClass.getAnnotation(TestParameters.class);
        if (annotation == null) {
            return defaultParameters();
        }
        return annotation;
    }

    @Override
    public Description getDescription() {
        Description suiteDescription = Description.createSuiteDescription(getTestClass());
        Description childDescription = childDescription();
        suiteDescription.addChild(childDescription);
        return suiteDescription;
    }

    private Description childDescription() {
        return Description.createTestDescription(getTestClass().getName(), testName, getTestClass().getAnnotations());
    }

    @Override
    public void run(RunNotifier notifier) {
        TestParameters parameters = getParameterAnnotation(getTestClass());
        if (parameters.parallelScenarios() != -1 && parameters.parallelScenarios() > 1) {
            runParallel(parameters, notifier);
            return;
        }
        notifier.fireTestStarted(childDescription());

        Class<?> actualTestClass = instrumentTestClass(getTestClass(), parameters);

        AtomicInteger executionCount = new AtomicInteger();
        long lastTime = System.currentTimeMillis();
        ExecutionGraph graph = new ExecutionGraph();
        LongStatistics results = new LongStatistics();
        OldCheckpointImpl initial = graph.computeCheckpointIfAbsent(-1, v -> new OldCheckpointImpl(v, "task-start"));
        OldCheckpointImpl end = graph.computeCheckpointIfAbsent(-2, v -> new OldCheckpointImpl(v, "finish"));
        try {
            do {
                if (doExecuteRunner(notifier, graph, executionCount, initial, end, parameters, actualTestClass, results)) {
                    return;
                }
                if (lastTime + 10000 < System.currentTimeMillis()) {
                    System.out.println(new Date() + ": " + executionCount.get() + " different paths found");
                    System.out.println("Result statistics: " + results);
                    System.out.println("Width: " + graph.getInitialNodeWidth());
                    System.out.println("-------------------------------------------------------------------------------------------------------------------");
                    lastTime = System.currentTimeMillis();
                }
            } while (graph.hasUnvisitedState());
            notifier.fireTestFinished(childDescription());
            System.out.println(executionCount.get() + " different executions path were checked");
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(childDescription(), e));
        }
    }

    public void runParallel(TestParameters parameters, RunNotifier notifier) {
        notifier.fireTestStarted(childDescription());
        Class<?> actualTestClass = instrumentTestClass(getTestClass(), parameters);
        AtomicInteger executionCount = new AtomicInteger();
        long lastTime = System.currentTimeMillis();
        String[] initialThreadNames = Arrays.stream(actualTestClass.getMethods()).filter(m -> m.isAnnotationPresent(Actor.class)).map(Method::getName).toArray(String[]::new);
        int n = parameters.parallelScenarios();
        Queue<String>[] preffixes = calculatePreffixes(initialThreadNames, n);
        ExecutionGraph[] graphs = new ExecutionGraph[preffixes.length];
        for (int i = 0; i < graphs.length; i++) {
            graphs[i] = new ExecutionGraph();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(graphs.length, new ThreadFactory() {

            private long executorCount = 0;

            @Override
            public synchronized Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "task-coordinator-" + executorCount++);
                thread.setDaemon(true);
                return thread;
            }
        });
        LongStatistics results = new LongStatistics();
        try {
            do {
                List<Future<Boolean>> futures = new ArrayList<>(n);

                for (int i = 0; i < preffixes.length; i++) {
                    Queue<String> preffix = new ArrayDeque<>(preffixes[i]);
                    ExecutionGraph graph = graphs[i];
                    if (hasUnvisitesNodes(graph, preffix)) {
                        OldCheckpointImpl initial = graph.computeCheckpointIfAbsent(-1, v -> new OldCheckpointImpl(v, "task-start"));
                        OldCheckpointImpl end = graph.computeCheckpointIfAbsent(-2, v -> new OldCheckpointImpl(v, "finish"));
                        futures.add(executorService.submit(() -> doExecuteRunner(notifier, graph, executionCount, initial, end, preffix, parameters, actualTestClass, results)));
                    }
                }
                if (futures.isEmpty()) {
                    break;
                }
                for (Future<Boolean> f : futures) {
                    try {
                        Boolean failureFound = f.get(parameters.runTimeoutSeconds(), TimeUnit.SECONDS);
                        if (failureFound) {
                            return;
                        }
                    } catch (ExecutionException e) {
                        notifier.fireTestFailure(new Failure(childDescription(), e.getCause()));
                        return;
                    } catch (TimeoutException e) {
                        System.err.println("Timeout waiting for task to complete. ");
                        notifier.fireTestFailure(new Failure(childDescription(), e));
                        f.cancel(true);
                        return;
                    }
                }
                if (lastTime + 10000 < System.currentTimeMillis()) {
                    System.out.println(new Date() + ": " + executionCount.get() + " different paths found");
                    for (int i = 0; i < preffixes.length; i++) {
                        System.out.println(preffixes[i] + " Has unvisited? " + hasUnvisitesNodes(graphs[i], preffixes[i]));
                    }
                    System.out.println("Result statistics: " + results);
                    System.out.println("-------------------------------------------------------------------------------------------------------------------");
                    lastTime = System.currentTimeMillis();
                }
            } while (true);
            notifier.fireTestFinished(childDescription());
            System.out.println(executionCount.get() + " different executions path were checked");
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(childDescription(), e));
        }
    }

    private List<Queue<String>> calculatePreffixesRecursive(List<Queue<String>> current, String[] initialThreadNames, int maxLength) {
        if (current.size() > maxLength) {
            return current;
        }
        List<Queue<String>> newCopy = new ArrayList<>(current.size() * initialThreadNames.length);
        for (var queue : current) {
            for (String newName : initialThreadNames) {
                Queue<String> nq = new LinkedList<>(queue);
                nq.add(newName);
                newCopy.add(nq);
            }
        }
        return calculatePreffixesRecursive(newCopy, initialThreadNames, maxLength);
    }
    @SuppressWarnings("unchecked")
    private Queue<String>[] calculatePreffixes(String[] initialThreadNames, int maxLength) {
        List<Queue<String>> newCopy = new ArrayList<>(initialThreadNames.length);
        for (String newName : initialThreadNames) {
            Queue<String> nq = new LinkedList<>();
            nq.add(newName);
            newCopy.add(nq);
        }

        List<Queue<String>> queues = calculatePreffixesRecursive(newCopy, initialThreadNames, maxLength);
        return queues.toArray(new Queue[queues.size()]);
    }

    public static Set<Class<?>> detectTouchedClasses(Set<Class<?>> alreadyKnown, Set<Class<?>> remaining, Set<Class<?>> excluded) {
        while (!remaining.isEmpty()) {
            Class<?> next = remaining.iterator().next();
            remaining.remove(next);
            if (alreadyKnown.contains(next) || excluded.contains(next)) {
                continue;
            }
            alreadyKnown.add(next);

            Set<Class<?>> alreadySeen = new HashSet<>(alreadyKnown);
            alreadySeen.addAll(excluded);
            ReadClassesVisitor visitor = new ReadClassesVisitor(alreadySeen);
            try {
                ClassReader reader = ASMUtils.readClass(next);
                if (reader != null) {
                    reader.accept(visitor, ClassReader.EXPAND_FRAMES);
                }
                for (Class<?> toCheck : visitor.getDiscoveredClasses()) {
                    if (toCheck.isPrimitive() || toCheck.isArray() || toCheck.getName().startsWith("java.") || toCheck.getName().startsWith("javax.")
                            || toCheck.getName().startsWith("jdk.internal.") || toCheck.getName().startsWith("com.sun.") || toCheck.getName().startsWith("sun.")
                            || toCheck.getName().startsWith("concurrencytest.") || toCheck.getName().startsWith("org.junit.") || alreadyKnown.contains(toCheck) || excluded.contains(toCheck)) {
                        continue;
                    }
                    remaining.add(toCheck);
                }
            } catch (IOException e) {
                System.err.println("Coult not find classfile for class: " + next);
            }
        }
        return alreadyKnown;
    }

    private Class<?> instrumentTestClass(Class<?> testClass, TestParameters parameters) {
        if (parameters.instrumentationStrategy() == InstrumentationStrategy.NONE) {
            return testClass;
        }
        Set<Class<?>> allClasses = new HashSet<>();
        Set<Class<?>> excludedClasses = new HashSet<>(Arrays.asList(parameters.excludedClasses()));
        allClasses.add(testClass);
        allClasses.addAll(Arrays.asList(parameters.instrumentedClasses()));
        if (parameters.autodetectClasses()) {
            allClasses = detectTouchedClasses(new HashSet<>(), allClasses, excludedClasses);
        }

        AtomicLong idGen = new AtomicLong();
        if (parameters.instrumentationStrategy() == InstrumentationStrategy.ATTACH_AGENT) {
            Agent.attachAndInstrument(allClasses);
            waitForAgent();
            if (agentError != null) {
                throw new RuntimeException(agentError);
            }
            return testClass;
        }


        String suffix = "_test";
        Map<String, String> names = new HashMap<>();
        Map<String, String> reverseNames = new HashMap<>();
        Map<String, Class<?>> originalNames = new HashMap<>();

        for (Class<?> t : allClasses) {
            String renamedInternalName = Type.getType(t).getInternalName() + suffix;
            names.put(Type.getType(t).getInternalName(), renamedInternalName);
            reverseNames.put(renamedInternalName, Type.getType(t).getInternalName());
            originalNames.put(renamedInternalName, t);
        }
        HashSet<String> unresolvedClassNames = new HashSet<>();
        SimpleRemapper simpleRemapper = new SimpleRemapper(names);
        this.loader = new OpenClassLoader();
        for (Class<?> excluded : parameters.excludedClasses()) {
            allClasses.remove(excluded);
        }
        for (Class<?> toRename : allClasses) {
            try {
                ClassReader reader = ASMUtils.readClass(toRename);
                if (reader == null) {
                    System.err.println("Could not find class file for class: " + toRename);
                    continue;
                }
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {

                    @Override
                    protected ClassLoader getClassLoader() {
                        return loader;
                    }

                    @Override
                    protected String getCommonSuperClass(String type1, String type2) {
                        // we will cheat a little bit here
                        if (type1.endsWith(suffix)) {
                            return getCommonSuperClass(reverseNames.get(type1), type2);
                        } else if (type2.endsWith(suffix)) {
                            return getCommonSuperClass(type1, reverseNames.get(type2));
                        }
                        String commonSuperClass = super.getCommonSuperClass(type1, type2);
                        if (names.containsKey(commonSuperClass)) {
                            return names.get(commonSuperClass);
                        }
                        return commonSuperClass;
                    }
                };
                CheckClassAdapter adapter = new CheckClassAdapter(writer, false);
                InjectCheckpointVisitor outer = new InjectCheckpointVisitor(adapter, toRename.getName(), idGen::incrementAndGet, originalNames, unresolvedClassNames, Arrays.asList(parameters.defaultCheckpoints()));
                ClassRemapper remapper = new ClassRemapper(outer, simpleRemapper);
                reader.accept(remapper, ClassReader.EXPAND_FRAMES);
                byte[] bytecode = writer.toByteArray();
                FileOutputStream fout = new FileOutputStream("/tmp/" + toRename.getName() + ".class");
                fout.write(bytecode);
                fout.close();
                loader.addClass(toRename.getName() + suffix, bytecode);
            } catch (TypeNotPresentException e) {
                //we will ignore those
            } catch (IOException e) {
                throw new RuntimeException("Could not find classfile for class: " + toRename);
            } catch (Throwable e) {
                throw new RuntimeException("Could not inject checkpoints on class: " + toRename, e);
            }
        }
        Thread.currentThread().setContextClassLoader(loader);
        try {
            Class<?> instrumentedClass = Class.forName(testClass.getName() + suffix, true, loader);
            System.out.println(idGen.get() + " checkpoints were inserted on " + allClasses.size() + " classes");
            return instrumentedClass;
        } catch (Exception e) {
            throw new RuntimeException("Could not load instrumented class:  " + testClass.getName() + suffix, e);
        }
    }

    private static synchronized void waitForAgent() {
        try {
            while (!agentDone) {
                ConcurrencyRunner.class.wait(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasUnvisitesNodes(ExecutionGraph graph, Queue<String> preffix) {
        Node node = graph.walk(preffix);
        return node == null || node.hasUnvisitedState();
    }

    private boolean hasUnvisitesNodes(ExecutionGraph graph, String node) {
        return graph.getInitialNode() == null || graph.getInitialNode().findNeighboor(node) == null || graph.getInitialNode().findNeighboor(node).hasUnvisitedState();
    }

    private boolean doExecuteRunner(RunNotifier notifier, ExecutionGraph graph, AtomicInteger executionCount, OldCheckpointImpl initial, OldCheckpointImpl end,
                                    TestParameters parameters, Class<?> actualTestClass, LongStatistics results) {
        try {
            final Object hostInstance = actualTestClass.getConstructor().newInstance();
            invokeBefore(hostInstance, notifier);
            Runnable invariants = () -> invokeAnnotatedMethods(hostInstance, Invariant.class, notifier);
            TestActor[] array = Arrays.stream(actualTestClass.getMethods()).filter(m -> m.isAnnotationPresent(Actor.class)).map(m -> this.toRunnable(m, hostInstance, initial, end, notifier)).toArray(TestActor[]::new);
            TestRuntimeImpl runtime = new TestRuntimeImpl(array, graph, invariants, executionCount.incrementAndGet(), parameters.maxLoopCount(), parameters.threadTimeoutSeconds(), parameters.randomPick(), loader);
            ExecutionDescription description = runtime.execute();
            try {
                if (description == null) {
                    return false;
                }
                if (description.isFailed()) {
                    System.err.println(description.createErrorDescription());
                    notifier.fireTestFailure(new Failure(childDescription(), description.getError()));
                    return true;
                }
                results.newSample(description.getPath().size());
                invokeAfter(hostInstance, notifier);
            } catch (AssertionError e) {
                notifier.fireTestFailure(new Failure(childDescription(), new AssertionError(description.createErrorDescription(), e)));
                return true;
            }
        } catch (InvocationTargetException e) {
            notifier.fireTestFailure(new Failure(childDescription(), e.getTargetException()));
            return true;
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(childDescription(), new RuntimeException("Unexpected error: " + e.getMessage(), e)));
            return true;
        }
        return false;
    }

    private boolean doExecuteRunner(RunNotifier notifier, ExecutionGraph graph, AtomicInteger executionCount, OldCheckpointImpl initial, OldCheckpointImpl end,
                                    Queue<String> pathPreffix, TestParameters parameters, Class<?> actualTestClass, LongStatistics results) {
        try {
            final Object hostInstance = actualTestClass.getConstructor().newInstance();
            invokeBefore(hostInstance, notifier);
            Runnable invariants = () -> invokeAnnotatedMethods(hostInstance, Invariant.class, notifier);
            TestActor[] array = Arrays.stream(actualTestClass.getMethods()).filter(m -> m.isAnnotationPresent(Actor.class)).map(m -> this.toRunnable(m, hostInstance, initial, end, notifier)).toArray(TestActor[]::new);
            TestRuntimeImpl runtime = new TestRuntimeImpl(array, graph, invariants, executionCount.incrementAndGet(), parameters.maxLoopCount(), parameters.threadTimeoutSeconds(), parameters.randomPick(), loader);
            ExecutionDescription description = runtime.resumeFrom(pathPreffix);
            try {
                if (description == null) {
                    return false;
                }
                if (description.isFailed()) {
                    System.err.println(description.createErrorDescription());
                    notifier.fireTestFailure(new Failure(childDescription(), description.getError()));
                    return true;
                }
                results.newSample(description.getPath().size());
                invokeAfter(hostInstance, notifier);
            } catch (AssertionError e) {
                notifier.fireTestFailure(new Failure(childDescription(), new AssertionError(description.createErrorDescription(), e)));
                return true;
            }
        } catch (InvocationTargetException e) {
            notifier.fireTestFailure(new Failure(childDescription(), e.getTargetException()));
            return true;
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(childDescription(), new RuntimeException("Unexpected error: " + e.getMessage(), e)));
            return true;
        }
        return false;
    }

    private void invokeAnnotatedMethods(Object hostInstance, Class<? extends Annotation> annotation, RunNotifier notifier) {
        Arrays.stream(hostInstance.getClass().getMethods()).filter(m -> m.isAnnotationPresent(annotation)).forEach(
                m -> {
                    try {
                        m.invoke(hostInstance);
                    } catch (IllegalAccessException e) {
                        notifier.fireTestFailure(new Failure(childDescription(), e));
                    } catch (InvocationTargetException e) {
                        if (e.getTargetException() instanceof AssertionError) {
                            throw (AssertionError) e.getTargetException();
                        }
                        notifier.fireTestFailure(new Failure(childDescription(), e.getTargetException()));
                    }
                }
        );

    }

    public static TestParameters defaultParameters() {
        return new TestParameters() {

            @Override
            public Class<?>[] excludedClasses() {
                return new Class[0];
            }

            @Override
            public boolean randomPick() {
                return false;
            }

            @Override
            public int parallelScenarios() {
                return -1;
            }

            @Override
            public CheckpointInjectionPoint[] defaultCheckpoints() {
                return new CheckpointInjectionPoint[]{CheckpointInjectionPoint.ALL};
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return TestParameters.class;
            }

            @Override
            public boolean autodetectClasses() {
                return true;
            }

            @Override
            public InstrumentationStrategy instrumentationStrategy() {
                return InstrumentationStrategy.RENAME;
            }

            @Override
            public Class<?>[] instrumentedClasses() {
                return new Class[0];
            }

            @Override
            public int maxLoopCount() {
                return TestRuntimeImpl.DEFAULT_MAX_LOOP_COUNT;
            }

            @Override
            public int threadTimeoutSeconds() {
                return TestRuntimeImpl.DEFAULT_ACTOR_TIMEOUT_SECONDS;
            }

            @Override
            public int runTimeoutSeconds() {
                return ConcurrencyRunner.DEFAULT_RUN_TIMEOUT_SECONDS;
            }
        };
    }

    private void invokeAfter(Object hostInstance, RunNotifier notifier) {
        invokeAnnotatedMethods(hostInstance, After.class, notifier);
    }

    private void invokeBefore(Object hostInstance, RunNotifier notifier) {
        invokeAnnotatedMethods(hostInstance, Before.class, notifier);
    }

    private TestActor toRunnable(Method method, Object hostInstance, OldCheckpointImpl initialCheckpoint, OldCheckpointImpl finishedCheckpoint, RunNotifier notifier) {
        try {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    ManagedThread currentThread = (ManagedThread) Thread.currentThread();
                    try {
                        currentThread.setCheckpointAndWait(initialCheckpoint);
                        if (Modifier.isSynchronized(method.getModifiers())) {
                            Object lockTarget = Modifier.isStatic(method.getModifiers()) ? hostInstance.getClass() : hostInstance;
                            String checkpointName = method.getDeclaringClass().getName() + "." + method.getName() + " ( before monitor acquired )";
                            TestRuntimeImpl.beforeMonitorAcquiredCheckpoint(lockTarget, runnerGeneratedCheckpointIds.decrementAndGet(), checkpointName, "");
                        }
                        method.invoke(hostInstance);
                    } catch (InvocationTargetException e) {
                        if (!(e.getTargetException() instanceof ThreadDisabled)) {
                            notifier.fireTestFailure(new Failure(childDescription(), new RuntimeException("Could not execute actor on method: " + method, e)));
                        }
                    } catch (ThreadDisabled e) {
                        //safe to ignore
                    } catch (Exception e) {
                        notifier.fireTestFailure(new Failure(childDescription(), new RuntimeException("Could not execute actor on method: " + method, e)));
                    } finally {
                        if (Modifier.isSynchronized(method.getModifiers())) {
                            Object lockTarget = Modifier.isStatic(method.getModifiers()) ? hostInstance.getClass() : hostInstance;
                            String checkpointName = method.getDeclaringClass().getName() + "." + method.getName() + " ( after monitor released )";
                            TestRuntimeImpl.afterMonitorReleasedCheckpoint(lockTarget, runnerGeneratedCheckpointIds.decrementAndGet(), checkpointName, "");
                        }
                        currentThread.setCheckpointAndWait(finishedCheckpoint);
                    }
                }
            };
            return new TestActor(method.getName(), task);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create TestActor for method: " + method, e);
        }
    }

    public Class<?> getTestClass() {
        return testClass;
    }

}
