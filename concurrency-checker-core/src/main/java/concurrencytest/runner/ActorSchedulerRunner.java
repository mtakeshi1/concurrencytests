package concurrencytest.runner;

import concurrencytest.LList;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.runtime.ManagedThread;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;

import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ActorSchedulerRunner {

    private final Tree explorationTree;
    private final CheckpointDurationConfiguration configuration;
    private final Queue<String> initialPathActorNames;
    private final int maxLoopCount;

    public ActorSchedulerRunner(Tree explorationTree, CheckpointDurationConfiguration configuration, Queue<String> initialPathActorNames, String mainTestClassName, URL[] classpath, int maxLoopCount) {
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
        RuntimeState state = initialState(mainTestObject);
        LList<String> path = LList.empty();
        String lastActor = null;
        TreeNode node = explorationTree.rootNode();
        long maxTime = System.nanoTime() + configuration.maxDurationPerRun().toNanos();
        while (!state.finished()) {
            detectDeadlock(path);
            Optional<String> nextActorToAdvance = selectNextActor(lastActor, node, state);
            if (nextActorToAdvance.isEmpty()) {
                throwNoPathError(path, lastActor, node, state);
            }
            ManagedThread selected = state.actorNamesToThreads().get(nextActorToAdvance);
            RuntimeState next = state.advance(selected, configuration.checkpointTimeout());
            node = node.advanced(state.threadStates().get(selected), next);
            lastActor = nextActorToAdvance.get();
            state = next;
            if (System.nanoTime() > maxTime) {
                throw new TimeoutException("max timeout (%dms) exceeded".formatted(configuration.maxDurationPerRun().toMillis()));
            }
        }
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

    private Optional<String> selectNextActor(String lastActor, TreeNode node, RuntimeState currentState) {
        Collection<String> paths = node.unexploredPaths().collect(Collectors.toSet());
        return currentState.runnableActors().filter(actor -> paths.contains(actor.getActorName()))
                .filter(actor -> actor.equals(lastActor) || currentState.actorNamesToThreadStates().get(actor).loopCount() < maxLoopCount)
                .findFirst().map(ManagedThread::getActorName);
    }

    private RuntimeState initialState(Object mainTestObject) {
        return null;
    }

    public static void main(String[] args) throws Exception {

    }

}
