package concurrencytest.asm;

import concurrencytest.asm.testClasses.SyncCallable;
import concurrencytest.checkpoint.MonitorCheckpoint;
import concurrencytest.util.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

public class SynchronizedMethodDeclarationTest extends BaseClassVisitorTest {

    @Test
    public void testSyncCallable() throws Exception {
        Class<?> injected = super.prepare(SyncCallable.class, (c, cv) -> new SynchronizedMethodDeclarationVisitor(cv, register, SyncCallable.class, ReflectionHelper.getInstance()));
        Method delegatedMethod = injected.getDeclaredMethod(SynchronizedMethodDeclarationVisitor.generateDelegateMethodName("call"));
        Assert.assertNotNull(delegatedMethod);
        Assert.assertTrue(Modifier.isPrivate(delegatedMethod.getModifiers()));
        Assert.assertTrue(Modifier.isFinal(delegatedMethod.getModifiers()));
        Assert.assertTrue(delegatedMethod.isSynthetic());
        Assert.assertFalse(Modifier.isSynchronized(delegatedMethod.getModifiers()));
        Method originalMethod = injected.getDeclaredMethod("call");
        Assert.assertFalse(Modifier.isSynchronized(originalMethod.getModifiers()));
        Object newInstance = injected.getConstructor().newInstance();
        Assert.assertTrue(newInstance instanceof Callable<?>);
    }

    @Test
    public void testSyncCallableWithSynchronization() throws Exception {
        Class<?> injected = super.prepare(SyncCallable.class, (c, cv) -> new SynchronizedMethodDeclarationVisitor(new SynchronizedBlockVisitor(
                cv, register, SyncCallable.class, ReflectionHelper.getInstance()
        ), register, SyncCallable.class, ReflectionHelper.getInstance()));
        Method delegatedMethod = injected.getDeclaredMethod(SynchronizedMethodDeclarationVisitor.generateDelegateMethodName("call"));
        Assert.assertNotNull(delegatedMethod);
        Assert.assertTrue(Modifier.isPrivate(delegatedMethod.getModifiers()));
        Assert.assertTrue(Modifier.isFinal(delegatedMethod.getModifiers()));
        Assert.assertTrue(delegatedMethod.isSynthetic());
        Assert.assertFalse(Modifier.isSynchronized(delegatedMethod.getModifiers()));
        Method originalMethod = injected.getDeclaredMethod("call");
        Assert.assertFalse(Modifier.isSynchronized(originalMethod.getModifiers()));
        Object newInstance = injected.getConstructor().newInstance();
        Assert.assertTrue(newInstance instanceof Callable<?>);
        Assert.assertEquals(8, register.allCheckpoints().size());
        Assert.assertTrue(register.allCheckpoints().values().stream().filter(s -> s instanceof MonitorCheckpoint).allMatch(s -> s.lineNumber() == -1));
    }

}
