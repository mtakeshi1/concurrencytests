package concurrencytest.asm;

import concurrencytest.asm.testClasses.InjectionTarget;
import concurrencytest.asm.testClasses.InjectionTarget2;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.description.ManualCheckpointImpl;
import concurrencytest.runtime.RecordingCheckpointRuntime;
import concurrencytest.reflection.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

public class ManualCheckpointTest extends BaseClassVisitorTest {

    @Test
    public void manualCheckpoint() throws Exception {
        Class<?> prepare = super.prepare(InjectionTarget.class, (c, delegate) -> new ManualCheckpointVisitor(delegate, register, c, ReflectionHelper.getInstance()));
        RecordingCheckpointRuntime runtime = execute(prepare.getConstructor().newInstance(), c -> ((Runnable) c).run());
        Assert.assertEquals(10, runtime.getCheckpoints().size());
        List<Checkpoint> checkpoints = register.allCheckpoints().values().stream().filter(s -> s.description() instanceof ManualCheckpointImpl).sorted(Comparator.comparingInt(Checkpoint::checkpointId)).toList();
        Assert.assertEquals(1, checkpoints.size());
        Checkpoint checkpoint = checkpoints.get(0);
//        Assert.assertTrue("should've been a manual checkpoint but was" + checkpoint.getClass(), checkpoint instanceof ManualCheckpointImpl);
        Assert.assertEquals(3, register.allCheckpoints().size());
        Assert.assertTrue(checkpoint.lineNumber() > 0);
        for (var state : runtime.getCheckpoints()) {
            Assert.assertEquals(checkpoint.description(), state.checkpoint());
            Assert.assertEquals("", state.details());
        }
    }


    @Test
    public void manualCheckpointWithContext() throws Exception {
        Class<?> prepare = super.prepare(InjectionTarget2.class, (c, delegate) -> new ManualCheckpointVisitor(delegate, register, c, ReflectionHelper.getInstance()));
        Assert.assertEquals(3, register.allCheckpoints().size());
        List<Checkpoint> checkpoints = register.allCheckpoints().values().stream().filter(s -> s.description() instanceof ManualCheckpointImpl).sorted(Comparator.comparingInt(Checkpoint::checkpointId)).toList();
        Assert.assertEquals(1, checkpoints.size());
        Checkpoint checkpoint = checkpoints.get(0);
        Assert.assertTrue(checkpoint.lineNumber() > 0);
        Assert.assertTrue(checkpoint.description() instanceof ManualCheckpointImpl);
        Object instance = prepare.getConstructor().newInstance();
        String value = String.valueOf(System.nanoTime());
        instance.getClass().getField("label").set(instance, value);
        RecordingCheckpointRuntime runtime = execute(instance, c -> ((Runnable) c).run());
        Assert.assertEquals(1, runtime.getCheckpoints().size());
        var state = runtime.getCheckpoints().get(0);
        Assert.assertEquals(checkpoint.description(), state.checkpoint());
        Assert.assertEquals(value, state.details());

    }

}
