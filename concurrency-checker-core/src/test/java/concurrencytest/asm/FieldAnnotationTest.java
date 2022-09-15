package concurrencytest.asm;

import concurrencytest.annotations.FieldCheckpoint;
import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.testClasses.InjectionTarget;
import concurrencytest.checkpoint.FieldAccessCheckpoint;
import concurrencytest.checkpoint.matcher.FieldAnnotationMatch;
import concurrencytest.runtime.RecordingCheckpointRuntime;
import concurrencytest.util.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

public class FieldAnnotationTest extends BaseClassVisitorTest {

    public Object injectFieldCheckpoints(Class<?> baseType, FieldCheckpoint checkpoint) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var config = new FieldAnnotationMatch(checkpoint);
        return injectFieldCheckpoints(baseType, config);
    }

    public Object injectFieldCheckpoints(Class<?> baseType, FieldAnnotationMatch config) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class<?> injected = super.prepare(baseType, (c, cv) -> new FieldCheckpointVisitor(config, cv, register, baseType, ReflectionHelper.getInstance()));
        Assert.assertNotNull(injected);
        return injected.getConstructor().newInstance();
    }

    @Test
    public void simpleInjectionAfter() throws Exception {
        FieldCheckpoint fc = new FieldAnnotationProxy() {
            @Override
            public Class<?> declaringClass() {
                return InjectionTarget.class;
            }

            @Override
            public InjectionPoint[] injectionPoints() {
                return new InjectionPoint[]{InjectionPoint.AFTER};
            }
        };
        Runnable target = (Runnable) injectFieldCheckpoints(InjectionTarget.class, fc);
        Assert.assertEquals(1, register.allCheckpoints().size());
        RecordingCheckpointRuntime managedRuntime = execute(target, Runnable::run);
        Assert.assertEquals(1, managedRuntime.getCheckpoints().size());
        FieldAccessCheckpoint after = (FieldAccessCheckpoint) managedRuntime.getCheckpoints().get(0).checkpoint();
        Assert.assertEquals("intPublicField", after.fieldName());
        Assert.assertEquals(InjectionPoint.AFTER, after.injectionPoint());
        Assert.assertTrue(after.fieldRead());
        Assert.assertFalse(after.fieldWrite());
        Assert.assertEquals(Integer.class, after.fieldType());
    }

    @Test
    public void simpleInjectionAfterDeclaringClass() throws Exception {
        FieldCheckpoint fc = new FieldAnnotationProxy() {

            @Override
            public Class<?> declaringClass() {
                return System.class;
            }

            @Override
            public BehaviourModifier[] behaviourModifiers() {
                return new BehaviourModifier[]{BehaviourModifier.STATIC, BehaviourModifier.FINAL};
            }

            @Override
            public InjectionPoint[] injectionPoints() {
                return new InjectionPoint[]{InjectionPoint.BEFORE};
            }
        };
        Runnable target = (Runnable) injectFieldCheckpoints(InjectionTarget.class, fc);
        Assert.assertEquals(1, register.allCheckpoints().size());
        RecordingCheckpointRuntime managedRuntime = execute(target, Runnable::run);
        Assert.assertEquals(1, managedRuntime.getCheckpoints().size());
        FieldAccessCheckpoint after = (FieldAccessCheckpoint) managedRuntime.getCheckpoints().get(0).checkpoint();
        Assert.assertEquals("out", after.fieldName());
        Assert.assertEquals(InjectionPoint.BEFORE, after.injectionPoint());
        Assert.assertTrue(after.fieldRead());
        Assert.assertFalse(after.fieldWrite());
        Assert.assertEquals(PrintStream.class, after.fieldType());
    }

    private static class FieldAnnotationProxy implements FieldCheckpoint {

        @Override
        public Class<?> declaringClass() {
            return Object.class;
        }

        @Override
        public String declaringClassNameRegex() {
            return ".+";
        }

        @Override
        public String fieldNameRegex() {
            return ".+";
        }

        @Override
        public InjectionPoint[] injectionPoints() {
            return new InjectionPoint[]{InjectionPoint.BEFORE, InjectionPoint.AFTER};
        }

        @Override
        public AccessModifier[] accessModifiers() {
            return AccessModifier.all();
        }

        @Override
        public BehaviourModifier[] behaviourModifiers() {
            return new BehaviourModifier[]{BehaviourModifier.STATIC, BehaviourModifier.SYNCHRONIZED, BehaviourModifier.VOLATILE, BehaviourModifier.TRANSIENT, BehaviourModifier.INSTANCE_MEMBER, BehaviourModifier.FINAL};
        }

        @Override
        public boolean fieldRead() {
            return true;
        }

        @Override
        public boolean fieldWrite() {
            return true;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return FieldCheckpoint.class;
        }
    }

    @Test
    public void simpleInjectionBeforeAfterPublicIntegerField() throws Exception {
        FieldCheckpoint fc = new FieldAnnotationProxy() {
            @Override
            public Class<?> declaringClass() {
                return InjectionTarget.class;
            }
        };
        Runnable target = (Runnable) injectFieldCheckpoints(InjectionTarget.class, fc);
        Assert.assertEquals(2, register.allCheckpoints().size());
        RecordingCheckpointRuntime managedRuntime = execute(target, Runnable::run);
        Assert.assertEquals(2, managedRuntime.getCheckpoints().size());
        FieldAccessCheckpoint before = (FieldAccessCheckpoint) managedRuntime.getCheckpoints().get(0).checkpoint();
        Assert.assertEquals("intPublicField", before.fieldName());
        Assert.assertEquals(InjectionPoint.BEFORE, before.injectionPoint());
        Assert.assertTrue(before.fieldRead());
        Assert.assertFalse(before.fieldWrite());
        Assert.assertEquals(Integer.class, before.fieldType());
        FieldAccessCheckpoint after = (FieldAccessCheckpoint) managedRuntime.getCheckpoints().get(1).checkpoint();
        Assert.assertNotEquals(before.checkpointId(), after.checkpointId());
        Assert.assertEquals("intPublicField", after.fieldName());
        Assert.assertEquals(InjectionPoint.AFTER, after.injectionPoint());
        Assert.assertTrue(after.fieldRead());
        Assert.assertFalse(after.fieldWrite());
        Assert.assertEquals(Integer.class, before.fieldType());
    }

}
