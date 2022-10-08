package concurrencytest.runner;

import concurrencytest.LList;
import concurrencytest.annotations.Actor;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.runtime.CheckpointRuntime;
import concurrencytest.runtime.MutableRuntimeState;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.ThreadState;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ActorSchedulerEntryPoint {

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

    private Collection<ActorBuilder> initialActors;

    private ScheduledExecutorService managedExecutorService;

    public void exploreAll() throws ActorSchedulingException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, TimeoutException {
        while (hasMorePathsToExplore()) {
            executeOnce();
        }
    }

    private boolean hasMorePathsToExplore() {
        Optional<TreeNode> node = walk(explorationTree.getOrInitializeRootNode(parseActorNames().keySet(), checkpointRegister), new LinkedList<>(initialPathActorNames));
        //TODO probably not enough
        return !node.map(TreeNode::allFinished).orElse(false);
    }

    private Optional<TreeNode> walk(TreeNode treeNode, Queue<String> initialPathActorNames) {
        if (initialPathActorNames.isEmpty()) {
            return Optional.of(treeNode);
        }
        throw new RuntimeException("not yet implemented");
    }

    public void executeOnce() throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ActorSchedulingException, TimeoutException {
        Object mainTestObject = instantiateMainTestClass();
        var initialActorNames = parseActorNames();
        RuntimeState runtime = initialState(initialActorNames);
        runtime.start(mainTestObject, configuration.checkpointTimeout());
        LList<String> path = LList.empty();
        String lastActor = null;
        TreeNode node = explorationTree.getOrInitializeRootNode(initialPathActorNames, checkpointRegister);
        long maxTime = System.nanoTime() + configuration.maxDurationPerRun().toNanos();
        Queue<String> preSelectedActorNames = new ArrayDeque<>(initialPathActorNames);
        while (!runtime.finished()) {
            detectDeadlock(path);
            Optional<String> nextActorToAdvance = selectNextActor(lastActor, node, runtime, preSelectedActorNames, maxLoopCount);
            if (nextActorToAdvance.isEmpty()) {
                // we are done with this path
                return;
            }
            path = path.prepend(nextActorToAdvance.get());
            ThreadState selected = runtime.actorNamesToThreadStates().get(nextActorToAdvance.get());
            RuntimeState next = runtime.advance(selected, configuration.checkpointTimeout());
            node = node.advance(selected, next);
            lastActor = nextActorToAdvance.get();
            runtime = next;
            if (System.nanoTime() > maxTime) {
                throw new TimeoutException("max timeout (%dms) exceeded".formatted(configuration.maxDurationPerRun().toMillis()));
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

    private Object instantiateMainTestClass() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return mainTestClass.getConstructor().newInstance();
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
        t.printStackTrace();
        this.actorError = t;
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

    public static void main(String[] args) {

    }

}
