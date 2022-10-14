package concurrencytest.runner;

import concurrencytest.LList;
import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.runtime.CheckpointRuntime;
import concurrencytest.runtime.MutableRuntimeState;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.ThreadState;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ActorSchedulerEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorSchedulerEntryPoint.class);

    private final Tree explorationTree;
    private final CheckpointDurationConfiguration configuration;
    private final List<String> initialPathActorNames;
    private final int maxLoopCount;
    private final Class<?> mainTestClass;
    private final CheckpointRegister checkpointRegister;
    private volatile Throwable actorError;

    public ActorSchedulerEntryPoint(Tree explorationTree, CheckpointRegister register, Configuration configuration, Class<?> mainTestClass) {
        this(explorationTree, register, configuration.durationConfiguration(), Collections.emptyList(), mainTestClass, configuration.maxLoopIterations());
    }

    public ActorSchedulerEntryPoint(Tree explorationTree, CheckpointRegister register, CheckpointDurationConfiguration configuration, List<String> initialPathActorNames, Class<?> mainTestClassName, int maxLoopCount) {
        this.explorationTree = explorationTree;
        this.checkpointRegister = register;
        this.configuration = configuration;
        this.initialPathActorNames = initialPathActorNames;
        this.maxLoopCount = maxLoopCount;
        this.mainTestClass = mainTestClassName;
    }

    private final Collection<CheckpointReachedCallback> callbacks = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService managedExecutorService;

    public Optional<Throwable> exploreAll() throws ActorSchedulingException, InterruptedException {
        try {
            invokeBeforeClass();
            while (hasMorePathsToExplore() && actorError == null) {
                executeOnce();
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
        Optional<TreeNode> node = walk(explorationTree.getOrInitializeRootNode(parseActorNames().keySet(), checkpointRegister), new LinkedList<>(initialPathActorNames));
        return !node.map(TreeNode::isFullyExplored).orElse(false);
    }

    private Optional<TreeNode> walk(TreeNode treeNode, Queue<String> initialPathActorNames) {
        if (initialPathActorNames.isEmpty()) {
            return Optional.of(treeNode);
        }
        throw new RuntimeException("not yet implemented");
    }

    public void executeOnce() throws InterruptedException, ActorSchedulingException {
        executeWithPreselectedPath(new ArrayDeque<>(initialPathActorNames));
    }

    public void executeWithPreselectedPath(Queue<String> preSelectedActorNames) throws InterruptedException, ActorSchedulingException {
        MDC.put("actor", "scheduler");
        Object mainTestObject = instantiateMainTestClass();
        var initialActorNames = parseActorNames();
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
                LOGGER.debug("Finished executiong with path: {}", runtime.getExecutionPath());
            }
        } catch (TimeoutException e) {
            LOGGER.warn("Timing out waiting for actors to converge.");
            LOGGER.warn("Execution path follows:\n" + String.join("\n", runtime.getExecutionPath()));
            reportActorError(e);
        }
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
        for (Method m : mainTestClass.getMethods()) {
            if (m.isAnnotationPresent(AfterActorsCompleted.class)) {
                try {
                    m.invoke(mainTestObject);
                } catch (IllegalAccessException e) {
                    reportActorError(new IllegalStateException("Security error trying to invoke @AfterActorsCompleted method: " + m + ". Is it public?", e));
                } catch (InvocationTargetException e) {
                    LOGGER.warn("@AfterActorCompleted %s threw %s (%s). Execution path follows:".formatted(m.getName(), e.getTargetException().getClass().getName(), e.getTargetException().getMessage()));
                    LOGGER.warn("Execution path follows:\n" + String.join("\n", runtime.getExecutionPath()));
                    reportActorError(e.getTargetException());
                }
            }
        }
    }

    private void callInvariants(Object mainTestObject, RuntimeState runtime) {
        for (Method m : mainTestClass.getMethods()) {
            if (m.isAnnotationPresent(Invariant.class)) {
                try {
                    m.invoke(mainTestObject);
                } catch (IllegalAccessException e) {
                    reportActorError(new IllegalStateException("Security error trying to invoke @Invariant method: " + m + ". Is it public?", e));
                } catch (InvocationTargetException e) {
                    LOGGER.warn("@Invariant %s threw %s (%s). Execution path follows:".formatted(m.getName(), e.getTargetException().getClass().getName(), e.getTargetException().getMessage()));
                    LOGGER.warn("Execution path follows:\n" + String.join("\n", runtime.getExecutionPath()));
                    reportActorError(e.getTargetException());
                }
            }
        }
    }


    private Map<String, Method> parseActorNames() {
        Map<String, Method> map = new HashMap<>();
        for (var m : mainTestClass.getMethods()) {
            Actor actor = m.getAnnotation(Actor.class);
            if (actor == null) {
                continue;
            }
            String actorName;
            if (actor.actorName().isEmpty()) {
                actorName = m.getName();
            } else {
                actorName = actor.actorName();
            }
            Method old = map.put(actorName, m);
            if (old != null) {
                throw new IllegalArgumentException("Two methods have the same actor name '%s': %s and %s".formatted(actorName, old, m));
            }
        }
        return map;
    }

    private CheckpointRuntime checkpointRuntime() {
        throw new RuntimeException("not yet implemented");
    }

    private void throwNoPathError(LList<String> path, String lastActor, TreeNode node, RuntimeState state) {
        //TODO
        throw new RuntimeException("no path from here - selected path: %s".formatted(path.reverse()));
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

    private void detectDeadlock(LList<String> path) {
//        throw new RuntimeException("no path from here - selected path: %s".formatted(path.reverse()));
    }

    public static Optional<String> selectNextActor(String lastActor, TreeNode node, RuntimeState currentState, Queue<String> preSelectedActorNames, int maxLoopCount) throws ActorSchedulingException {
        if (node.isFullyExplored()) {
            return Optional.empty();
        }
        if (!preSelectedActorNames.isEmpty()) {
            return Optional.of(preSelectedActorNames.poll());
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
            throw maxLoopViolation.map(actor -> (ActorSchedulingException) new MaxLoopCountViolationException(actor, maxLoopCount)).orElse(new NoRunnableActorFoundException(unexploredNodes, Collections.emptyList()));
        }
        runnableActors.retainAll(unexploredNodes);
        return runnableActors.stream().findAny();
    }

    protected void reportActorError(Throwable t) {
        LOGGER.debug("Error thrown", t);
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
