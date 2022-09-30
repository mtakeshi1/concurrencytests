package concurrencytest.runner;

import concurrencytest.LList;
import concurrencytest.annotations.Actor;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runtime.CheckpointRuntime;
import concurrencytest.runtime.MutableRuntimeState;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.tree.ThreadState;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ActorSchedulerEntryPoint {

    private final Tree explorationTree;
    private final CheckpointDurationConfiguration configuration;
    private final Queue<String> initialPathActorNames;
    private final int maxLoopCount;
    private final String mainTestClass;
    private final CheckpointRegister checkpoingRegister;
    private volatile Throwable actorError;

    public ActorSchedulerEntryPoint(Tree explorationTree, CheckpointRegister register, CheckpointDurationConfiguration configuration, Queue<String> initialPathActorNames, String mainTestClassName, int maxLoopCount) {
        this.explorationTree = explorationTree;
        this.checkpoingRegister = register;
        this.configuration = configuration;
        this.initialPathActorNames = initialPathActorNames;
        this.maxLoopCount = maxLoopCount;
        this.mainTestClass = mainTestClassName;
    }

    private final Collection<CheckpointReachedCallback> callbacks = new CopyOnWriteArrayList<>();

    private Collection<ActorBuilder> initialActors;

    private ScheduledExecutorService managedExecutorService;

    public void executeOnce() throws Exception {
        Object mainTestObject = instantiateMainTestClass();
        var initialActorNames = parseActorNames();
        RuntimeState runtime = initialState(mainTestObject, checkpoingRegister, initialActorNames);
        LList<String> path = LList.empty();
        String lastActor = null;
        TreeNode node = explorationTree.rootNode();
        long maxTime = System.nanoTime() + configuration.maxDurationPerRun().toNanos();
        Queue<String> preSelectedActorNames = new ArrayDeque<>(initialPathActorNames);
        while (!runtime.finished()) {
            detectDeadlock(path);
            try {
                Optional<String> nextActorToAdvance = selectNextActor(lastActor, node, runtime, preSelectedActorNames, maxLoopCount);
                if (nextActorToAdvance.isEmpty()) {
                    // we are done with this path
                    return;
                }
                ThreadState selected = runtime.actorNamesToThreadStates().get(nextActorToAdvance.get());
                RuntimeState next = runtime.advance(selected, configuration.checkpointTimeout());
                node = node.advance(runtime.actorNamesToThreadStates().get(selected.actorName()), next);
                lastActor = nextActorToAdvance.get();
                runtime = next;
                if (System.nanoTime() > maxTime) {
                    throw new TimeoutException("max timeout (%dms) exceeded".formatted(configuration.maxDurationPerRun().toMillis()));
                }
            } catch (ActorSchedulingException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, Method> parseActorNames() {
        Map<String, Method> map = new HashMap<>();
        for (var m : ReflectionHelper.getInstance().resolveName(this.mainTestClass).getMethods()) {
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

    private Object instantiateMainTestClass() throws Exception {
        return ReflectionHelper.getInstance().resolveName(this.mainTestClass).getConstructor().newInstance();
    }

    private void detectDeadlock(LList<String> path) {
        throw new RuntimeException("no path from here - selected path: %s".formatted(path.reverse()));
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

    private RuntimeState initialState(Object mainTestObject, CheckpointRegister checkpointRegister, Map<String, Method> initialActorNames) {
        Map<String, Runnable> managedThreadRunnables = new HashMap<>();
        initialActorNames.forEach((actor, method) -> {
            Object[] params = collectParametersForActorRun(mainTestObject);
            Runnable wrapped = () -> {
                try {
                    method.invoke(params);
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
        return new MutableRuntimeState(checkpointRegister, managedThreadRunnables);

    }

    private Object[] collectParametersForActorRun(Object mainTestObject) {

        return new Object[]{mainTestObject};
    }

    public static void main(String[] args) throws Exception {

    }

}
