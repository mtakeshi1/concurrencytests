package concurrencytest.asm;

import concurrencytest.asm.testClasses.SyncBlockTarget;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.checkpoint.MonitorCheckpointReached;
import concurrencytest.runtime.RecordingCheckpointRuntime;
import concurrencytest.reflection.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;

public class SynchronizedBlockTest extends BaseClassVisitorTest {


    @Test
    public void monitorCheckpointTest() throws Exception {
        Class<?> prepare = super.prepare(SyncBlockTarget.class, (c, delegate) -> new SynchronizedBlockVisitor(delegate, register, c, ReflectionHelper.getInstance()));
        RecordingCheckpointRuntime runtime = execute(prepare.getConstructor().newInstance(), c -> ((Runnable) c).run());
        Assert.assertEquals(4, runtime.getCheckpoints().size());
        CheckpointReached checkpointReached = runtime.getCheckpoints().get(0);
        Assert.assertTrue(checkpointReached instanceof MonitorCheckpointReached);
        MonitorCheckpointReached mon = (MonitorCheckpointReached) checkpointReached;
        Assert.assertTrue(mon.checkpoint().lineNumber() > 0);
    }


}
