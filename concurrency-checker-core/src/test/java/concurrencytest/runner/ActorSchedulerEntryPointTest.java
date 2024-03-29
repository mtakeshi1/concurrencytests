package concurrencytest.runner;

import concurrencytest.asm.SynchronizedBlockVisitor;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.runner.statistics.MutableRunStatistics;
import concurrencytest.runtime.impl.MutableRuntimeState;
import concurrencytest.runtime.thread.ManagedThread;
import concurrencytest.runtime.tree.ActorInformation;
import concurrencytest.runtime.tree.TreeNode;
import concurrencytest.v2.test.SimpleSharedCounter;
import concurrencytest.v2.test.SingleActorTest;
import concurrencytest.v2.test.SynchronizedMethodCounter;
import concurrencytest.v2.test.TwoActorsVolatileRead;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * These tests are intended to verify the {@link ActorSchedulerEntryPoint}
 * <p>
 * It should be registering the checkpoints and running the tests should make the actors reach the checkpoints.
 * <p>
 * The tree should also be explored.
 */
public class ActorSchedulerEntryPointTest extends BaseRunnerTest {

    private ThreadPoolExecutor threadPoolExecutor;

    @Before
    public void setupLog() {
        Logger global = Logger.getGlobal();
        global.setLevel(Level.ALL);
        global.setUseParentHandlers(false);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        global.addHandler(consoleHandler);
        Logger.getLogger(MutableRuntimeState.class.getName()).setLevel(Level.FINEST);
        ThreadFactory threadFactory = r -> {
            ManagedThread mt = new ManagedThread(r);
            mt.setDaemon(true);
            mt.setSchedulerName("test-scheduler-");
            return mt;
        };
        this.threadPoolExecutor = new ThreadPoolExecutor(1, 10, 99999, TimeUnit.MINUTES, new SynchronousQueue<>(), threadFactory);
    }

    @After
    public void tearDown() {
        if (this.threadPoolExecutor != null) {
            threadPoolExecutor.shutdownNow();
        }
    }

    @Test
    public void testSchedulerSingleActor() {
        ActorSchedulerEntryPoint point = super.prepare(new BasicConfiguration(SingleActorTest.class));
        CheckpointRegister checkpointRegister = point.getCheckpointRegister();
        Assert.assertEquals(checkpointRegister.checkpointsById().values().stream().map(String::valueOf).collect(Collectors.joining("\n")), 6, checkpointRegister.allCheckpoints().size());
        point.executeOnce(new MutableRunStatistics(), threadPoolExecutor);
        Optional<TreeNode> maybeNode = point.getExplorationTree().getRootNode();
        Assert.assertTrue(maybeNode.isPresent());
        TreeNode treeNode = maybeNode.get();
        Assert.assertEquals(1, treeNode.threads().size());
        String actorName = "actor1";
        Assert.assertEquals(actorName, treeNode.threads().keySet().iterator().next());
        ActorInformation information = treeNode.threads().values().iterator().next();
        Assert.assertEquals(actorName, information.actorName());
        Assert.assertEquals(checkpointRegister.taskStartingCheckpoint().checkpointId(), information.checkpointId());
        Assert.assertEquals(0, information.loopCount());
        assertNotBlockedNoLocks(information);
        Assert.assertTrue(treeNode.isFullyExplored());

        //level 1
        maybeNode = treeNode.childNode(actorName).map(Supplier::get);
        Assert.assertTrue(maybeNode.isPresent());
        treeNode = maybeNode.get();
        Assert.assertEquals(1, treeNode.threads().size());
        Assert.assertEquals(actorName, treeNode.threads().keySet().iterator().next());
        information = treeNode.threads().values().iterator().next();

        Assert.assertEquals(actorName, information.actorName());
        Assert.assertEquals(4, information.checkpointId());
        Assert.assertEquals(0, information.loopCount());
        assertNotBlockedNoLocks(information);
        Assert.assertTrue(treeNode.isFullyExplored());

        //level 2
        maybeNode = treeNode.childNode(actorName).map(Supplier::get);
        Assert.assertTrue(maybeNode.isPresent());
        treeNode = maybeNode.get();
        Assert.assertEquals(1, treeNode.threads().size());
        Assert.assertEquals(actorName, treeNode.threads().keySet().iterator().next());
        information = treeNode.threads().values().iterator().next();

        Assert.assertEquals(actorName, information.actorName());
        Assert.assertEquals(5, information.checkpointId());
        Assert.assertEquals(0, information.loopCount());
        assertNotBlockedNoLocks(information);
        Assert.assertTrue(treeNode.isFullyExplored());

        //level 3
        maybeNode = treeNode.childNode(actorName).map(Supplier::get);
        Assert.assertTrue(maybeNode.isPresent());
        treeNode = maybeNode.get();
        Assert.assertEquals(1, treeNode.threads().size());
        Assert.assertEquals(actorName, treeNode.threads().keySet().iterator().next());
        information = treeNode.threads().values().iterator().next();

        Assert.assertEquals(actorName, information.actorName());
        Assert.assertEquals(3, information.checkpointId());
        Assert.assertEquals(0, information.loopCount());
        assertNotBlockedNoLocks(information);
        Assert.assertTrue(treeNode.allFinished());
        Assert.assertTrue(treeNode.isFullyExplored());
    }

    @Test
    public void sharedCounterError() {
        ActorSchedulerEntryPoint point = super.prepare(new BasicConfiguration(SimpleSharedCounter.class));
        CheckpointRegister checkpointRegister = point.getCheckpointRegister();
        Assert.assertEquals(checkpointRegister.checkpointsById().values().stream().map(String::valueOf).collect(Collectors.joining("\n")), 10, checkpointRegister.allCheckpoints().size()); // it must include access on the @invariant
        point.executeWithPreselectedPath(new ArrayDeque<>(List.of("actor1", "actor2")), new MutableRunStatistics(), threadPoolExecutor);
        Optional<TreeNode> maybeNode = point.getExplorationTree().getRootNode();
        Assert.assertTrue(maybeNode.isPresent());
        TreeNode treeNode = maybeNode.get();
        Assert.assertEquals(3, treeNode.threads().size());
        Throwable t = point.getReportedError();
        Assert.assertTrue(t instanceof AssertionError);
        Assert.assertFalse(treeNode.isFullyExplored());
    }

    @Test
    public void testSchedulerTwoActorsVolatileRead() {
        ActorSchedulerEntryPoint point = super.prepare(new BasicConfiguration(TwoActorsVolatileRead.class));
        CheckpointRegister checkpointRegister = point.getCheckpointRegister();
        String checkpoints = checkpointRegister.checkpointsById().values().stream().map(String::valueOf).collect(Collectors.joining("\n"));
        System.out.println(checkpoints);
        Assert.assertEquals(checkpoints, 4, checkpointRegister.allCheckpoints().size());
        point.executeOnce(new MutableRunStatistics(), threadPoolExecutor);

        Optional<TreeNode> maybeNode = point.getExplorationTree().getRootNode();
        Assert.assertTrue(maybeNode.isPresent());
        TreeNode treeNode = maybeNode.get();
        Assert.assertEquals(2, treeNode.threads().size());
        Assert.assertFalse(treeNode.isFullyExplored());
        Assert.assertEquals(4, treeNode.maxKnownDepth());
        for (int i = 0; i < 4; i++) {
            point.executeOnce(new MutableRunStatistics(), threadPoolExecutor);
            maybeNode = point.getExplorationTree().getRootNode();
            Assert.assertTrue(maybeNode.isPresent());
            treeNode = maybeNode.get();
            Assert.assertEquals(4, treeNode.maxKnownDepth());
            Assert.assertFalse("shouldn't have fully explored after %d explorations".formatted(i + 1), treeNode.isFullyExplored());
        }
        point.executeOnce(new MutableRunStatistics(), threadPoolExecutor);
        maybeNode = point.getExplorationTree().getRootNode();
        Assert.assertTrue(maybeNode.isPresent());
        treeNode = maybeNode.get();
        Assert.assertEquals(4, treeNode.maxKnownDepth());
        Assert.assertTrue(treeNode.isFullyExplored());
    }

    @Test
    public void testSimpleSynchronizedBlocks() {
        ActorSchedulerEntryPoint point = super.prepare(new BasicConfiguration(SynchronizedMethodCounter.class));
        CheckpointRegister checkpointRegister = point.getCheckpointRegister();
        String checkpoints = checkpointRegister.checkpointsById().values().stream().map(String::valueOf).collect(Collectors.joining("\n"));
        System.out.println(checkpoints);
        Assert.assertEquals(checkpoints, SynchronizedBlockVisitor.BEFORE_EXIT_CHECKPOINT ? 11 : 10, checkpointRegister.allCheckpoints().size());
        point.executeOnce(new MutableRunStatistics(), threadPoolExecutor);
        Optional<TreeNode> rootNode = point.getExplorationTree().getRootNode();
        Assert.assertTrue(rootNode.isPresent());
        TreeNode node = rootNode.get();
        Assert.assertEquals(2, node.threads().size());
        Assert.assertNull(point.getReportedError());

    }

}