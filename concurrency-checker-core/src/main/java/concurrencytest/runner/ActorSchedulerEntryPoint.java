package concurrencytest.runner;

import concurrencytest.LList;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.runtime.CheckpointRuntime;
import concurrencytest.runtime.ManagedThread;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.tree.ThreadState;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;

import java.net.URL;
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

    public ActorSchedulerEntryPoint(Tree explorationTree, CheckpointDurationConfiguration configuration, Queue<String> initialPathActorNames, String mainTestClassName, URL[] classpath, int maxLoopCount) {
        this.explorationTree = explorationTree;
        this.configuration = configuration;
        this.initialPathActorNames = initialPathActorNames;
        this.maxLoopCount = maxLoopCount;
    }

    private final Collection<CheckpointReachedCallback> callbacks = new CopyOnWriteArrayList<>();

    private Collection<ActorBuilder> initialActors;

    private ScheduledExecutorService managedExecutorService;

    public void executeOnce() throws Exception {
        Object mainTestObject = instantiateMainTestClass();
        CheckpointRuntime checkpointRuntime = checkpointRuntime();
        RuntimeState runtime = initialState(mainTestObject, checkpointRuntime);
        LList<String> path = LList.empty();
        String lastActor = null;
        TreeNode node = explorationTree.rootNode();
        long maxTime = System.nanoTime() + configuration.maxDurationPerRun().toNanos();
        Queue<String> preSelectedActorNames = new ArrayDeque<>(initialPathActorNames);
        while (!runtime.finished()) {
            detectDeadlock(path);
            Optional<String> nextActorToAdvance = selectNextActor(lastActor, node, runtime, preSelectedActorNames, maxLoopCount);
            if (nextActorToAdvance.isEmpty()) {
                // we are done with this path
                return;
            }
            ThreadState selected = runtime.actorNamesToThreadStates().get(nextActorToAdvance.get());
            RuntimeState next = runtime.advance(selected, configuration.checkpointTimeout(), checkpointRuntime);
            node = node.advance(runtime.actorNamesToThreadStates().get(selected.actorName()), next);
            lastActor = nextActorToAdvance.get();
            runtime = next;
            if (System.nanoTime() > maxTime) {
                throw new TimeoutException("max timeout (%dms) exceeded".formatted(configuration.maxDurationPerRun().toMillis()));
            }
        }
    }

    private CheckpointRuntime checkpointRuntime() {
        throw new RuntimeException("not yet implemented");
    }

    private void throwNoPathError(LList<String> path, String lastActor, TreeNode node, RuntimeState state) {
        //TODO
        throw new RuntimeException("no path from here - selected path: %s".formatted(path.reverse()));
    }

    private Object instantiateMainTestClass() {
        return null;
    }

    private void detectDeadlock(LList<String> path) {

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

    private RuntimeState initialState(Object mainTestObject, CheckpointRuntime checkpointRuntime) {
        return null;
    }

    public static void main(String[] args) throws Exception {

    }

}
