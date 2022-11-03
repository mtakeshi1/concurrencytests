package concurrencytest.runner;

import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.runtime.CheckpointRuntime;
import concurrencytest.runtime.MutableRuntimeState;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ActorSchedulerEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorSchedulerEntryPoint.class);

    private final Tree explorationTree;
    private final CheckpointDurationConfiguration configuration;
    private final List<String> initialPathActorNames;
    private final int maxLoopCount;
    private final Class<?> mainTestClass;
    private final CheckpointRegister checkpointRegister;
    private final String schedulerName;
    private volatile Throwable actorError;

    public ActorSchedulerEntryPoint(Tree explorationTree, CheckpointRegister register, Configuration configuration, Class<?> mainTestClass) {
        this(explorationTree, register, configuration.durationConfiguration(), Collections.emptyList(), mainTestClass, configuration.maxLoopIterations(), "scheduler");
    }

    public ActorSchedulerEntryPoint(Tree explorationTree, CheckpointRegister register, CheckpointDurationConfiguration configuration, List<String> initialPathActorNames, Class<?> mainTestClassName, int maxLoopCount, String schedulerName) {
        this.explorationTree = explorationTree;
        this.checkpointRegister = register;
        this.configuration = configuration;
        this.initialPathActorNames = initialPathActorNames;
        this.maxLoopCount = maxLoopCount;
        this.mainTestClass = mainTestClassName;
        this.schedulerName = schedulerName;
    }

    private final Collection<CheckpointReachedCallback> callbacks = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService managedExecutorService;

    public Optional<Throwable> exploreAll(Consumer<TreeNode> treeObserver, RunStatistics stat) throws InterruptedException {
        try {
            MDC.put("actor", schedulerName);
            invokeBeforeClass();
            while (hasMorePathsToExplore() && actorError == null) {
                executeOnce(stat);
                TreeNode node = explorationTree.getOrInitializeRootNode(ActorSchedulerSetup.parseActorMethods(mainTestClass).keySet(), checkpointRegister);
                treeObserver.accept(node);
            }
            return Optional.ofNullable(actorError);
        } finally {
            invokeCleanup();
        }
    }

    private void invokeBeforeClass() {
        for (Method m : mainTestClass.getMethods()) {
            if (m.isAnnotationPresent(BeforeClass.class)) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    reportActorError(new IllegalArgumentException("@BeforeClass method should be public and static - " + m));
                    break;
                }
                try {
                    m.invoke(null);
                } catch (IllegalAccessException e) {
                    reportActorError(new IllegalStateException("Security error trying to invoke @BeforeClass method: " + m + ". Is it public and static?", e));
                } catch (InvocationTargetException e) {
                    reportActorError(e.getTargetException());
                }
            }
        }
    }

    private void invokeCleanup() {
        for (Method m : mainTestClass.getMethods()) {
            if (m.isAnnotationPresent(AfterClass.class)) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    reportActorError(new IllegalArgumentException("@AfterClass method should be public and static - " + m));
                    break;
                }
                try {
                    m.invoke(null);
                } catch (IllegalAccessException e) {
                    reportActorError(new IllegalStateException("Security error trying to invoke @AfterClass method: " + m + ". Is it public and static?", e));
                } catch (InvocationTargetException e) {
                    reportActorError(e.getTargetException());
                }
            }
        }
    }

    private boolean hasMorePathsToExplore() {
        Optional<TreeNode> node = walk(explorationTree.getOrInitializeRootNode(ActorSchedulerSetup.parseActorMethods(mainTestClass).keySet(), checkpointRegister), new LinkedList<>(initialPathActorNames));
        return !node.map(TreeNode::isFullyExplored).orElse(false);
    }

    private Optional<TreeNode> walk(TreeNode treeNode, Queue<String> initialPathActorNames) {
        if (initialPathActorNames.isEmpty()) {
            return Optional.of(treeNode);
        }
        Optional<TreeNode> next = Optional.of(treeNode);
        while (!initialPathActorNames.isEmpty()) {
            var childNode = initialPathActorNames.remove();
            next = next.flatMap(s -> s.childNode(childNode)).map(Supplier::get);
        }
        return next;
    }

    public void executeOnce(RunStatistics stat) throws InterruptedException {
        executeWithPreselectedPath(new ArrayDeque<>(initialPathActorNames), stat);
    }

    public void executeWithPreselectedPath(Queue<String> preSelectedActorNames, RunStatistics stat) throws InterruptedException {
        MDC.put("actor", "scheduler");
        long t0 = System.nanoTime();
        Object mainTestObject = instantiateMainTestClass();
        var initialActorNames = ActorSchedulerSetup.parseActorMethods(mainTestClass);
        RuntimeState runtime = initialState(initialActorNames);

        try {
            runtime.start(mainTestObject, configuration.checkpointTimeout());
            String lastActor = null;
            TreeNode node = explorationTree.getOrInitializeRootNode(initialActorNames.keySet(), checkpointRegister);
            invokeBefore(mainTestObject);
            long maxTime = System.nanoTime() + configuration.maxDurationPerRun().toNanos();
            runtime.errorReported().ifPresent(this::reportActorError);
            while (!runtime.finished() && actorError == null) {
                callInvariants(mainTestObject, runtime);
                Optional<String> nextActorToAdvance = selectNextActor(lastActor, node, runtime, preSelectedActorNames, maxLoopCount);
                if (nextActorToAdvance.isEmpty()) {
                    // we are done with this path
                    throw new RuntimeException("?");
                }
                ThreadState selected = runtime.actorNamesToThreadStates().get(nextActorToAdvance.get());
                RuntimeState next = runtime.advance(selected, configuration.checkpointTimeout());
                node = node.advance(selected, next);
                lastActor = nextActorToAdvance.get();
                runtime = next;
                if (System.nanoTime() > maxTime) {
                    throw new TimeoutException("max timeout (%dms) exceeded".formatted(configuration.maxDurationPerRun().toMillis()));
                }
                runtime.errorReported().ifPresent(this::reportActorError);
            }
            if (actorError == null) {
                callEndOfActors(mainTestObject, runtime);
                node.markFullyExplored();
                long duration = System.nanoTime() - t0;
                stat.record(duration, runtime.getExecutionPath().size());
                LOGGER.debug("Finished executiong in {}ns with path: {}", duration, runtime.getExecutionPath());
            }
            callJUnitAfter(mainTestObject, runtime);
        } catch (TimeoutException e) {
            LOGGER.warn("Timing out waiting for actors to converge.");
            LOGGER.warn("Execution path follows:\n" + String.join("\n", runtime.getExecutionPath()));
            reportActorError(e);
        } catch (ActorSchedulingException e) {
            LOGGER.warn("Scheduling error - either a deadlock or starvation");
            LOGGER.warn("Execution path follows:\n" + String.join("\n", runtime.getExecutionPath()));
            reportActorError(e);
        }
    }

    private void callJUnitAfter(Object mainTestObject, RuntimeState runtime) {
        invokeMethodsWithAnnotation(mainTestObject, runtime, After.class);
    }

    private void invokeBefore(Object mainTestObject) {
        for (Method m : mainTestClass.getMethods()) {
            if (m.isAnnotationPresent(Before.class)) {
                try {
                    m.invoke(mainTestObject);
                } catch (IllegalAccessException e) {
                    reportActorError(new IllegalStateException("Security error trying to invoke @Before method: " + m + ". Is it public?", e));
                } catch (InvocationTargetException e) {
                    reportActorError(e.getTargetException());
                }
            }
        }
    }

    private void callEndOfActors(Object mainTestObject, RuntimeState runtime) {
        invokeMethodsWithAnnotation(mainTestObject, runtime, AfterActorsCompleted.class);
    }

    private void invokeMethodsWithAnnotation(Object mainTestObject, RuntimeState runtime, Class<? extends Annotation> desiredAnnotation) {
        for (Method m : mainTestClass.getMethods()) {
            if (m.isAnnotationPresent(desiredAnnotation)) {
                try {
                    m.invoke(mainTestObject);
                } catch (IllegalAccessException e) {
                    reportActorError(new IllegalStateException("Security error trying to invoke method: %s with annotation %s. Is it public?".formatted(m, desiredAnnotation.getName()), e));
                } catch (InvocationTargetException e) {
                    LOGGER.warn("@AfterActorCompleted %s threw %s (%s). Execution path follows:".formatted(m.getName(), e.getTargetException().getClass().getName(), e.getTargetException().getMessage()));
                    LOGGER.warn("Execution path follows:\n%s".formatted(String.join("\n", runtime.getExecutionPath())));
                    reportActorError(e.getTargetException());
                }
            }
        }
    }

    private void callInvariants(Object mainTestObject, RuntimeState runtime) {
        invokeMethodsWithAnnotation(mainTestObject, runtime, Invariant.class);
    }


    private CheckpointRuntime checkpointRuntime() {
        throw new RuntimeException("not yet implemented");
    }

    private Object instantiateMainTestClass() {
        try {
            return mainTestClass.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("%s threw InstantiationException. Is it a non-abstract class? Does it have a public constructor?".formatted(mainTestClass), e);
        } catch (IllegalAccessException | SecurityException | NoSuchMethodException e) {
            throw new IllegalArgumentException("%s threw %s. Is it public? Does it have a public constructor?".formatted(mainTestClass, e.getClass()), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("constructor for %s threw InvocationTargetException".formatted(mainTestClass), e.getTargetException());
        }
    }

    public static Optional<String> selectNextActor(String lastActor, TreeNode node, RuntimeState currentState, Queue<String> preSelectedActorNames, int maxLoopCount) throws ActorSchedulingException {
        if (node.isFullyExplored()) {
            return Optional.empty();
        }
        if (!preSelectedActorNames.isEmpty()) {
            String selected = preSelectedActorNames.poll();
            ThreadState state = currentState.actorNamesToThreadStates().get(selected);
            if (!state.canProceed(currentState)) {
                throw new InitialPathBlockedException("actor named '%s' from preselected path cannot proceed.".formatted(selected));
            }
            return Optional.of(selected);
        }
        var runnableActors = currentState.runnableActors()
                .filter(actor -> !actor.actorName().equals(lastActor) || currentState.actorNamesToThreadStates().get(actor.actorName()).loopCount() < maxLoopCount)
                .filter(ts -> !ts.finished())
                .map(ThreadState::actorName).collect(Collectors.toSet());
        var unexploredNodes = node.unexploredPaths().collect(Collectors.toSet());
        if (runnableActors.isEmpty()) {
            //we've reached a deadlock
            Optional<String> maxLoopViolation = currentState.runnableActors()
                    .filter(actor -> !actor.actorName().equals(lastActor) || currentState.actorNamesToThreadStates().get(actor.actorName()).loopCount() >= maxLoopCount)
                    .map(ThreadState::actorName).findAny();
            findCircularDependency(currentState);
            throw maxLoopViolation.map(actor -> (ActorSchedulingException) new MaxLoopCountViolationException(actor, maxLoopCount)).orElse(new NoRunnableActorFoundException(unexploredNodes, Collections.emptyList()));
        }
        runnableActors.retainAll(unexploredNodes);
        return runnableActors.stream().findAny();
    }

    public static void findCircularDependency(RuntimeState currentState) throws DeadlockFoundException {
        Map<String, Set<String>> directDependencies = new HashMap<>();
        for (var actor : currentState.allActors()) {
            actor.blockedBy().ifPresent($ -> $.blockedBy(currentState).forEach(ts -> directDependencies.computeIfAbsent(actor.actorName(), ignored -> new HashSet<>()).add(ts.actorName())));
        }

        Set<String> actorNames = new HashSet<>(directDependencies.keySet());
        for (var ac : actorNames) {
            Set<String> visited = new HashSet<>();
            visited.add(ac);
            exploreRecursive(ac, directDependencies, visited);
            directDependencies.remove(ac);
        }
    }

    public static void exploreRecursive(String initial, Map<String, Set<String>> directDependencies, Set<String> visited) throws DeadlockFoundException {
        Set<String> dependencies = directDependencies.getOrDefault(initial, Collections.emptySet());
        for (var dep : dependencies) {
            if (visited.contains(dep)) {
                // deadlock found
                throw new DeadlockFoundException("actor '%s' depends on '%s' which depends on '%s' again".formatted(initial, dep, initial));
            }
            visited.add(dep);
            exploreRecursive(dep, directDependencies, visited);
        }

    }

    protected void reportActorError(Throwable t) {
        if (!(t instanceof AssertionError)) {
            LOGGER.debug("Error thrown", t);
        }
        if (actorError == null) {
            this.actorError = t;
        } else {
            actorError.addSuppressed(t);
        }
    }

    private RuntimeState initialState(Map<String, Method> initialActorNames) {
        Map<String, Consumer<Object>> managedThreadRunnables = new HashMap<>();
        initialActorNames.forEach((actor, method) -> {
            Object[] params = collectParametersForActorRun();
            Consumer<Object> wrapped = callTarget -> {
                try {
                    method.invoke(callTarget, params);
                } catch (IllegalAccessException e) {
                    reportActorError(e);
                } catch (InvocationTargetException e) {
                    reportActorError(e.getTargetException());
                } catch (RuntimeException | Error e) {
                    reportActorError(e);
                    throw e;
                }
            };
            managedThreadRunnables.put(actor, wrapped);
        });
        return new MutableRuntimeState(this.checkpointRegister, managedThreadRunnables);

    }

    private Object[] collectParametersForActorRun() {
        return new Object[]{};
    }

    public Tree getExplorationTree() {
        return explorationTree;
    }

    public CheckpointDurationConfiguration getConfiguration() {
        return configuration;
    }

    public CheckpointRegister getCheckpointRegister() {
        return checkpointRegister;
    }

    public Throwable getReportedError() {
        return actorError;
    }
}
