package concurrencytest.runner;

import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.Invariant;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.runner.statistics.MutableRunStatistics;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.exception.RunAbortedException;
import concurrencytest.runtime.exception.SchedulerAbortedException;
import concurrencytest.runtime.impl.MutableRuntimeState;
import concurrencytest.runtime.thread.ManagedThread;
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
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ActorSchedulerEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorSchedulerEntryPoint.class);

    private final Tree explorationTree;
    private final Configuration configuration;
    private final List<String> initialPathActorNames;
    private final int maxLoopCount;
    private final Class<?> mainTestClass;
    private final CheckpointRegister checkpointRegister;
    private final String schedulerName;
    private final Consumer<Throwable> errorReporter;
    private final TaskSchedulerInterface scheduler;
    private volatile Throwable actorError;

    private final Collection<CheckpointReachedCallback> callbacks = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService managedExecutorService;

    public ActorSchedulerEntryPoint(Tree explorationTree, CheckpointRegister register, Configuration configuration, Class<?> mainTestClass, Consumer<Throwable> errorReporter, TaskSchedulerInterface schedulerInterface) {
        this(explorationTree, register, configuration, Collections.emptyList(), mainTestClass, configuration.maxLoopIterations(), "scheduler", errorReporter, schedulerInterface);
    }

    public ActorSchedulerEntryPoint(Tree explorationTree, CheckpointRegister register, Configuration configuration, List<String> initialPathActorNames, Class<?> mainTestClass,
                                    int maxLoopCount, String schedulerName, Consumer<Throwable> externalErrorReporter, TaskSchedulerInterface schedulerInterface) {
        this.explorationTree = explorationTree;
        this.checkpointRegister = register;
        this.configuration = configuration;
        this.initialPathActorNames = initialPathActorNames;
        this.maxLoopCount = maxLoopCount;
        this.mainTestClass = mainTestClass;
        this.schedulerName = schedulerName;

        this.errorReporter = externalErrorReporter;
        this.scheduler = schedulerInterface;
    }


    public Optional<Throwable> exploreAll(Consumer<TreeNode> treeObserver, MutableRunStatistics stat) {
        ThreadFactory threadFactory = r -> {
            ManagedThread mt = new ManagedThread(r);
            mt.setSchedulerName(schedulerName);
            return mt;
        };

        int size = ActorSchedulerSetup.parseInitialActorNames(mainTestClass).size();
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(size, size, Long.MAX_VALUE, TimeUnit.MINUTES, new ArrayBlockingQueue<>(size), threadFactory);
        try {
            MDC.put("actor", schedulerName);
            invokeBeforeClass();
            while (hasMorePathsToExplore() && actorError == null) {
                executeOnce(stat, executorService);
                TreeNode node = explorationTree.getOrInitializeRootNode(ActorSchedulerSetup.parseActorMethods(mainTestClass).keySet(), checkpointRegister);
                treeObserver.accept(node);
            }
            if (actorError != null) {
                errorReporter.accept(actorError);
            }
            return Optional.ofNullable(actorError);
        } catch (SchedulerAbortedException t) {
            LOGGER.debug("scheduler: " + schedulerName + " finished");
            return Optional.empty();
        } catch (Throwable t) {
            errorReporter.accept(t);
            return Optional.of(t);
        } finally {
            invokeCleanup();
            List<Runnable> list = executorService.shutdownNow();
            if (!list.isEmpty()) {
                LOGGER.warn("Executor for scheduler: %s shutdown but %d tasks still remain".formatted(this.schedulerName, list.size()));
            }
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
        /*
         * FIXME
         *  this condition is wrong. We need something to convey:
         * - either the tree is fully explored
         * - every non-explored node is scheduled by someone other than me
         */
        Optional<TreeNode> node = walk(explorationTree.getOrInitializeRootNode(
                        ActorSchedulerSetup.parseActorMethods(mainTestClass).keySet(), checkpointRegister),
                new LinkedList<>(initialPathActorNames));
        return !node.map(TreeNode::isFullyExplored).orElse(false);
    }

    private Optional<TreeNode> walk(TreeNode treeNode, Queue<String> initialPathActorNames) {
        if (initialPathActorNames.isEmpty()) {
            return Optional.of(treeNode);
        }
        Optional<TreeNode> next = Optional.of(treeNode);
        while (!initialPathActorNames.isEmpty()) {
            var childNode = initialPathActorNames.remove();
            if (next.map(TreeNode::isFullyExplored).orElse(false)) {
                return Optional.of(TreeNode.EMPTY_TREE_NODE);
            }
            next = next.flatMap(s -> s.childNode(childNode)).map(Supplier::get);
        }
        return next;
    }

    public void executeOnce(MutableRunStatistics stat, ThreadPoolExecutor executor) {
        executeWithPreselectedPath(new ArrayDeque<>(initialPathActorNames), stat, executor);
    }

    public void executeWithPreselectedPath(Queue<String> preSelectedActorNames, MutableRunStatistics stat, ThreadPoolExecutor executorService) {
        MDC.put("actor", schedulerName);
        Thread.currentThread().setName(schedulerName);
        long t0 = System.nanoTime();
        Object mainTestObject = instantiateMainTestClass();
        var initialActorNames = ActorSchedulerSetup.parseActorMethods(mainTestClass);

        RuntimeState runtime = initialState(initialActorNames, executorService);
        Collection<Future<Throwable>> actorTasks = Collections.emptyList(); //FIXME change this to Future<Void>
        List<String> pathSoFar = new ArrayList<>();
        try {
            actorTasks = runtime.start(mainTestObject, configuration.durationConfiguration().checkpointTimeout());
            String lastActor = null;
            TreeNode node = explorationTree.getOrInitializeRootNode(initialActorNames.keySet(), checkpointRegister);
            invokeBefore(mainTestObject);
            long maxTime = System.nanoTime() + configuration.durationConfiguration().maxDurationPerRun().toNanos();
            runtime.errorReported().ifPresent(this::reportActorError);
            while (!runtime.finished() && actorError == null) {
                callInvariants(mainTestObject, runtime);
                Collection<String> options = allAvailableActors(lastActor, node, runtime, preSelectedActorNames, maxLoopCount);
                if (options.isEmpty()) {
                    cancelTasks(actorTasks);
                    throw new RunAbortedException();
                }
                // we will attempt to fork
                Iterator<String> iterator = options.iterator();
                var myChosenPath = iterator.next();
                if (scheduler.canFork()) {
                    while (iterator.hasNext()) {
                        var next = iterator.next();
                        List<String> copy = new ArrayList<>(pathSoFar);
                        copy.add(next);
                        if (scheduler.spawnTasks(spawner -> spawner.spawn(copy))) {
                            node.markLinkAsStartingPoint(next);
                        }
                    }
                }
                ThreadState selected = runtime.actorNamesToThreadStates().get(myChosenPath);
                RuntimeState next = runtime.advance(selected, configuration.durationConfiguration().checkpointTimeout());
                node = node.advance(selected, next);
                lastActor = myChosenPath;
                pathSoFar.add(myChosenPath);
                runtime = next;
                if (System.nanoTime() > maxTime) {
                    throw new TimeoutException("max timeout (%dms) exceeded".formatted(configuration.durationConfiguration().maxDurationPerRun().toMillis()));
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
            for (Future<?> f : actorTasks) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    cancelTasks(actorTasks);
                    reportActorError(e.getCause());
                }
            }
        } catch (CancellationException | RunAbortedException | InterruptedException | RejectedExecutionException e) {
            LOGGER.trace("Task cancelled");
            cancelTasks(actorTasks);
        } catch (SchedulerAbortedException e) {
            LOGGER.trace("Scheduler aborted");
            cancelTasks(actorTasks);
            throw e;
        } catch (TimeoutException e) {
            LOGGER.warn("Timing out waiting for actors to converge.");
            LOGGER.warn("Execution path follows:\n" + String.join("\n", runtime.getExecutionPath()));
            reportActorError(e);
            cancelTasks(actorTasks);
        } catch (InitialPathBlockedException e) {
            // ignore for now
            cancelTasks(actorTasks);
//            reportActorError(e);
        } catch (ActorSchedulingException e) {
            LOGGER.warn("Scheduling error - either a deadlock or starvation");
            LOGGER.warn("Execution path follows:\n" + String.join("\n", runtime.getExecutionPath()));
            reportActorError(e);
            cancelTasks(actorTasks);
        } catch (Throwable t) {
            reportActorError(t);
            LOGGER.warn("Unexpected error waiting for actor tasks: %s".formatted(t.getMessage()), t);
            cancelTasks(actorTasks);
        } finally {
            //FIXME insert timeout here
            while (executorService.getActiveCount() != 0) {
                LockSupport.parkNanos(100);
            }
            scheduler.notifyTaskFinished();
        }
    }

    private void cancelTasks(Collection<Future<Throwable>> actorTasks) {
        for (var fut : actorTasks) {
            fut.cancel(true);
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

    public static Collection<String> allAvailableActors(String lastActor, TreeNode node, RuntimeState currentState, Queue<String> preSelectedActorNames, int maxLoopCount) throws ActorSchedulingException {
        if (node.isFullyExplored()) {
            /*
             * I know this is going to bother me later, as it has many times already.
             *
             * This can happen on multi threaded cases (parallel executions > 0) where the marking of the fully visited node started after
             * this node tried to determine if the tree is commpletely exausted. In this case, in th middle of the exploration, the conditions (fully explored)
             * can change to true so we return empty and let the upstream caller deal
             */
            return List.of();
        }
        if (!preSelectedActorNames.isEmpty()) {
            String selected = preSelectedActorNames.poll();
            ThreadState state = currentState.actorNamesToThreadStates().get(selected);
            if (!state.canProceed(currentState)) {
                throw new InitialPathBlockedException("actor named '%s' from preselected path cannot proceed.".formatted(selected));
            }
            return List.of(selected);
        }
        var runnableActors = currentState.runnableActors()
                .filter(actor -> currentState.actorNamesToThreadStates().get(actor.actorName()).loopCount() < maxLoopCount)
                .filter(ts -> !ts.finished())
                .map(ThreadState::actorName).collect(Collectors.toSet());
        var unexploredNodes = node.unexploredPaths().collect(Collectors.toSet());
        if (runnableActors.isEmpty()) {
            //we've reached a deadlock
            Optional<String> maxLoopViolation = currentState.runnableActors()
                    .map(ThreadState::actorName)
                    .filter(key -> currentState.actorNamesToThreadStates().get(key).loopCount() >= maxLoopCount).findAny();
            findCircularDependency(currentState);
            throw maxLoopViolation.map(actor -> (ActorSchedulingException) new MaxLoopCountViolationException(actor, maxLoopCount)).orElse(new NoRunnableActorFoundException(unexploredNodes, Collections.emptyList()));
        }
        if (unexploredNodes.isEmpty()) {
            node.checkAllChildrenExplored(); // help marking the node and parent nodes as
            throw new RunAbortedException();
        }
        runnableActors.retainAll(unexploredNodes);
        return runnableActors;
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
        if (!(t instanceof InterruptedException) && !(t instanceof CancellationException) && !(t instanceof RunAbortedException)) {
            errorReporter.accept(t);
            if (actorError == null) {
                this.actorError = t;
            } else if (actorError != t) {
                actorError.addSuppressed(t);
            }
        }
    }

    private RuntimeState initialState(Map<String, Function<Object, Throwable>> initialActorNames, ThreadPoolExecutor executorService) {
        return new MutableRuntimeState(this.checkpointRegister, initialActorNames, this::reportActorError, executorService, this.configuration);
    }

    private Object[] collectParametersForActorRun() {
        return new Object[]{};
    }

    public Tree getExplorationTree() {
        return explorationTree;
    }

    public CheckpointDurationConfiguration getDurationConfiguration() {
        return configuration.durationConfiguration();
    }

    public CheckpointRegister getCheckpointRegister() {
        return checkpointRegister;
    }

    public Throwable getReportedError() {
        return actorError;
    }
}
