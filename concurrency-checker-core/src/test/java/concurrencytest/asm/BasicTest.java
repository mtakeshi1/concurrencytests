package concurrencytest.asm;

import concurrencytest.asm.testClasses.InjectionTarget;
import concurrencytest.asm.testClasses.SyncCallableMonitor;
import concurrencytest.checkpoint.StandardCheckpointRegister;
import concurrencytest.reflection.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

public class BasicTest extends BaseClassVisitorTest {

    @Test
    public void testNoOp() throws Exception {
        Class<?> prepared = prepare(InjectionTarget.class, (t, cv) -> cv);
        Runnable run = (Runnable) prepared.getConstructor().newInstance();
        run.run();
    }

    @Test
    public void testRemoveSyncModifier() throws Exception {
        Class<?> aClass = prepare(SyncCallableMonitor.class, ((targetClass, delegate) -> new BaseClassVisitor(delegate, new StandardCheckpointRegister(), SyncCallableMonitor.class, ReflectionHelper.getInstance()) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("call")) {
                    return super.visitMethod(Modifier.PUBLIC, name, descriptor, signature, exceptions);
                } else {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }
        }));
        Object instance = aClass.getConstructor().newInstance();
        try {
            ((Callable<?>) instance).call();
            Assert.fail("should've thrown IllegalMonitorStateException");
        } catch (IllegalMonitorStateException e) {
            // should have
        }
    }
}
