package concurrencytest.asm;

import concurrencytest.asm.testClasses.InjectionTarget;
import concurrencytest.asm.testClasses.InjectionTarget2;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.ManualCheckpointImpl;
import org.junit.Assert;
import org.junit.Test;

public class ManualCheckpointTest extends BaseClassVisitorTest {

    @Test
    public void manualCheckpointWithArg() throws Exception {
        Class<?> prepare = super.prepare(InjectionTarget.class, (c, delegate) -> new ManualCheckpointVisitor(delegate, register, c));
        ManagedRuntime runtime = execute(prepare.getConstructor().newInstance(), c -> ((Runnable) c).run());
        Assert.assertEquals(10, runtime.getCheckpoints().size());
        Checkpoint checkpoint = register.allCheckpoints().values().iterator().next();
        Assert.assertTrue(checkpoint instanceof ManualCheckpointImpl);
        Assert.assertEquals(1, register.allCheckpoints().size());
        for (var state : runtime.getCheckpoints()) {
            Assert.assertEquals(checkpoint, state.checkpoint());
            Assert.assertEquals("", state.details());
        }
    }


    @Test
    public void manualCheckpointEmpty() throws Exception {
        Class<?> prepare = super.prepare(InjectionTarget2.class, (c, delegate) -> new ManualCheckpointVisitor(delegate, register, c));
        Assert.assertEquals(1, register.allCheckpoints().size());
        Checkpoint checkpoint = register.allCheckpoints().values().iterator().next();
        Assert.assertTrue(checkpoint instanceof ManualCheckpointImpl);
        Object instance = prepare.getConstructor().newInstance();
        String value = String.valueOf(System.nanoTime());
        instance.getClass().getField("label").set(instance, value);
        ManagedRuntime runtime = execute(instance, c -> ((Runnable) c).run());
        Assert.assertEquals(1, runtime.getCheckpoints().size());
        var state = runtime.getCheckpoints().get(0);
        Assert.assertEquals(checkpoint, state.checkpoint());
        Assert.assertEquals(value, state.details());

    }

}
