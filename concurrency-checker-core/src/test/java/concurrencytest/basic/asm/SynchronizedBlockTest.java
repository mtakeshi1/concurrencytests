package concurrencytest.basic.asm;

import concurrencytest.asm.SynchronizedBlockVisitor;
import concurrencytest.asm.WaitParkWakeupVisitor;
import concurrencytest.basic.asm.testClasses.SyncBlockTarget;
import concurrencytest.basic.asm.testClasses.WaitNotifyTestTarget;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.checkpoint.instance.MonitorCheckpointReached;
import concurrencytest.checkpoint.instance.NotifySignalCheckpoint;
import concurrencytest.checkpoint.instance.WaitCheckpointReached;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runner.RecordingCheckpointRuntime;
import org.junit.Assert;
import org.junit.Test;

public class SynchronizedBlockTest extends BaseClassVisitorTest {

    @Test
    public void monitorCheckpointTest() throws Exception {
        Class<?> prepare = super.prepare(SyncBlockTarget.class, (c, delegate) -> new SynchronizedBlockVisitor(delegate, register, c, ReflectionHelper.getInstance()));
        RecordingCheckpointRuntime runtime = execute(prepare.getConstructor().newInstance(), c -> ((Runnable) c).run());
        Assert.assertEquals(SynchronizedBlockVisitor.BEFORE_EXIT_CHECKPOINT ? 4 : 3, runtime.getCheckpoints().size());
        CheckpointReached checkpointReached = runtime.getCheckpoints().get(0);
        Assert.assertTrue(checkpointReached instanceof MonitorCheckpointReached);
        MonitorCheckpointReached mon = (MonitorCheckpointReached) checkpointReached;
        Assert.assertTrue(mon.checkpoint().lineNumber() > 0);
    }

    @Test
    public void monitorWaitNotifyTest() throws Exception {
        var enhanced = prepare(WaitNotifyTestTarget.class, (c, delegate) -> new WaitParkWakeupVisitor(delegate, register, c, ReflectionHelper.getInstance()));
        RecordingCheckpointRuntime runtime = execute(enhanced.getConstructor().newInstance(), c -> ((Runnable) c).run());
        Assert.assertEquals(4, runtime.getCheckpoints().size());
        Assert.assertEquals(2, runtime.getCheckpoints().stream().filter(i -> i instanceof NotifySignalCheckpoint).count());
        Assert.assertEquals(2, runtime.getCheckpoints().stream().filter(i -> i instanceof WaitCheckpointReached).count());
    }
}
