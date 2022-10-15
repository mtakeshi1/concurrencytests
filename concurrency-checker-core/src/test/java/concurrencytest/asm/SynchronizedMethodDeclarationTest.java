package concurrencytest.asm;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.testClasses.SyncCallable;
import concurrencytest.checkpoint.description.MonitorCheckpointDescription;
import concurrencytest.reflection.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

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
        Assert.assertTrue(originalMethod.isAnnotationPresent(Actor.class));
        Assert.assertFalse("did not preserve original @Actor annotation", Modifier.isSynchronized(originalMethod.getModifiers()));
        Object newInstance = injected.getConstructor().newInstance();
        Assert.assertTrue(newInstance instanceof Callable<?>);
    }

    @Test
    public void testSyncCallableWithSynchronization() throws Exception {
        Class<?> injected = super.prepare(SyncCallable.class, (c, cv) -> new SynchronizedMethodDeclarationVisitor(new SynchronizedBlockVisitor(cv, register, SyncCallable.class, ReflectionHelper.getInstance()), register, SyncCallable.class, ReflectionHelper.getInstance()));
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
        Assert.assertEquals(6, register.allCheckpoints().size());
        Assert.assertTrue(monitorCheckpoints().allMatch(s -> s.lineNumber() == -1));
        Assert.assertTrue("should have BEFORE monitor acquire", monitorCheckpoints().filter(MonitorCheckpointDescription::monitorAcquire).toList().stream().anyMatch(s -> s.injectionPoint() == InjectionPoint.BEFORE));
        Assert.assertTrue("should have AFTER monitor acquire", monitorCheckpoints().filter(MonitorCheckpointDescription::monitorAcquire).toList().stream().anyMatch(s -> s.injectionPoint() == InjectionPoint.AFTER));
        Assert.assertTrue("should have BEFORE monitor release", monitorCheckpoints().filter(MonitorCheckpointDescription::isMonitorRelease).toList().stream().anyMatch(s -> s.injectionPoint() == InjectionPoint.BEFORE));
        Assert.assertTrue("should have AFTER monitor release", monitorCheckpoints().filter(MonitorCheckpointDescription::isMonitorRelease).toList().stream().anyMatch(s -> s.injectionPoint() == InjectionPoint.AFTER));
//        Assert.assertTrue("monitor type should be SyncCallable but was: " + monitorCheckpoints().map(CheckpointDescription::details).filter(s -> !s.equals(SyncCallable.class.getName())).collect(Collectors.toList()), monitorCheckpoints().allMatch(s -> s.details().equals(SyncCallable.class.getName())));
    }

    private Stream<MonitorCheckpointDescription> monitorCheckpoints() {
        return register.allCheckpoints().values().stream().filter(s -> s.description() instanceof MonitorCheckpointDescription).map(s -> (MonitorCheckpointDescription) s.description());
    }

}
