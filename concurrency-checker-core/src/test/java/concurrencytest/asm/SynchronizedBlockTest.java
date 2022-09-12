package concurrencytest.asm;

import concurrencytest.asm.testClasses.SyncBlockTarget;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.MonitorCheckpoint;
import concurrencytest.runtime.CheckpointReached;
import concurrencytest.runtime.MonitorCheckpointReached;
import concurrencytest.runtime.RecordingCheckpointRuntime;
import concurrencytest.util.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

public class SynchronizedBlockTest extends BaseClassVisitorTest {


    @Test
    public void monitorCheckpointTest() throws Exception {
        Class<?> prepare = super.prepare(SyncBlockTarget.class, (c, delegate) -> new SynchronizedBlockVisitor(delegate, register, c, ReflectionHelper.getInstance()));
        Set<Checkpoint> set = register.allCheckpoints().values().stream().filter(checkpoint -> !(checkpoint instanceof MonitorCheckpoint)).collect(Collectors.toSet());
        Assert.assertTrue(set.toString(), set.isEmpty());
        RecordingCheckpointRuntime runtime = execute(prepare.getConstructor().newInstance(), c -> ((Runnable) c).run());
        Assert.assertEquals(4, runtime.getCheckpoints().size());
        CheckpointReached checkpointReached = runtime.getCheckpoints().get(0);
        Assert.assertTrue(checkpointReached instanceof MonitorCheckpointReached);
        MonitorCheckpointReached mon = (MonitorCheckpointReached) checkpointReached;
//        Assert.assertTrue();
    }


}
