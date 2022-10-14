package concurrencytest.runner;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.runtime.MutableRuntimeState;
import concurrencytest.runtime.tree.ActorInformation;
import concurrencytest.runtime.tree.TreeNode;
import concurrencytest.v2.test.SimpleSharedCounter;
import concurrencytest.v2.test.SingularActorTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ActorSchedulerEntryPointTest extends BaseRunnerTest {

    @Before
    public void setupLog() {
        Logger global = Logger.getGlobal();
        global.setLevel(Level.ALL);
        global.setUseParentHandlers(false);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        global.addHandler(consoleHandler);
        Logger.getLogger(MutableRuntimeState.class.getName()).setLevel(Level.FINEST);
    }

    @Test
    public void testSchedulerSingleActor() throws ActorSchedulingException, InterruptedException, TimeoutException {
        ActorSchedulerEntryPoint point = super.prepare(new BasicConfiguration(SingularActorTest.class));
        CheckpointRegister checkpointRegister = point.getCheckpointRegister();
        Assert.assertEquals(checkpointRegister.checkpointsById().values().stream().map(String::valueOf).collect(Collectors.joining("\n")), 6, checkpointRegister.allCheckpoints().size());
        point.executeOnce();
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
    public void doubleActorTest() throws ActorSchedulingException, InterruptedException, TimeoutException {
        ActorSchedulerEntryPoint point = super.prepare(new BasicConfiguration(SimpleSharedCounter.class));
        CheckpointRegister checkpointRegister = point.getCheckpointRegister();
        Assert.assertEquals(checkpointRegister.checkpointsById().values().stream().map(String::valueOf).collect(Collectors.joining("\n")), 8, checkpointRegister.allCheckpoints().size()); // it must include access on the @invariant
        point.executeWithPreselectedPath(new ArrayDeque<>(List.of("actor1", "actor2")));
        Optional<TreeNode> maybeNode = point.getExplorationTree().getRootNode();
        Assert.assertTrue(maybeNode.isPresent());
        TreeNode treeNode = maybeNode.get();
        Assert.assertEquals(2, treeNode.threads().size());
        Throwable t = point.getReportedError();
        Assert.assertTrue(t instanceof AssertionError);
    }

//    @Test
//    public void testSchedulerTwoActorsVolatileRead() throws ActorSchedulingException, InterruptedException, TimeoutException {
//        ActorSchedulerEntryPoint point = super.prepare(new BasicConfiguration(TwoActorsVolatileRead.class));
//        CheckpointRegister checkpointRegister = point.getCheckpointRegister();
//        String checkpoints = checkpointRegister.checkpointsById().values().stream().map(String::valueOf).collect(Collectors.joining("\n"));
//        System.out.println(checkpoints);
//        Assert.assertEquals(checkpoints, 4, checkpointRegister.allCheckpoints().size());
//        point.executeOnce();
//
//        Optional<TreeNode> maybeNode = point.getExplorationTree().getRootNode();
//        Assert.assertTrue(maybeNode.isPresent());
//        TreeNode treeNode = maybeNode.get();
//        Assert.assertEquals(2, treeNode.threads().size());
//        Assert.assertFalse(treeNode.isFullyExplored());
//        Assert.assertEquals(4, treeNode.maxKnownDepth());
//        for(int i = 0; i < 4; i++) {
//            point.executeOnce();
//            maybeNode = point.getExplorationTree().getRootNode();
//            Assert.assertTrue(maybeNode.isPresent());
//            treeNode = maybeNode.get();
//            Assert.assertEquals(4, treeNode.maxKnownDepth());
//            Assert.assertFalse("shouldn't have explored after %d explorations".formatted(i + 1), treeNode.isFullyExplored());
//        }
//        point.executeOnce();
//        maybeNode = point.getExplorationTree().getRootNode();
//        Assert.assertTrue(maybeNode.isPresent());
//        treeNode = maybeNode.get();
//        Assert.assertEquals(4, treeNode.maxKnownDepth());
//        Assert.assertTrue(treeNode.isFullyExplored());
//
//    }
}