package concurrencytest.basic.asm;

import concurrencytest.asm.SynchronizedBlockVisitor;
import concurrencytest.basic.asm.testClasses.SyncBlockTarget;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.checkpoint.instance.MonitorCheckpointReached;
import concurrencytest.runner.RecordingCheckpointRuntime;
import concurrencytest.reflection.ReflectionHelper;
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


}
