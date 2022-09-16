package concurrencytest.asm;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.testClasses.Example;
import concurrencytest.asm.testClasses.MethodInvTestsTarget;
import concurrencytest.checkpoint.MethodCallCheckpoint;
import concurrencytest.config.MethodInvocationMatcher;
import concurrencytest.runtime.CheckpointReached;
import concurrencytest.runtime.RecordingCheckpointRuntime;
import concurrencytest.reflection.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

public class MethodInvocationTest extends BaseClassVisitorTest {

    @Test
    public void checkpointWithArguments() throws Exception {
        Class<?> target = prepare(MethodInvTestsTarget.class, (c, delegate) -> {
            MethodInvocationMatcher matcher = (classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) -> List.class.isAssignableFrom(invocationTargetType);
            return new MethodInvocationVisitor(delegate, register, c, ReflectionHelper.getInstance(), matcher);
        });
        Assert.assertEquals(10, register.allCheckpoints().size());
        RecordingCheckpointRuntime runtime = execute(target.getConstructor().newInstance(), e -> ((Example) e).a());
        Assert.assertEquals(2, runtime.getCheckpoints().size());
        CheckpointReached before = runtime.getCheckpoints().get(0);
        assertMethodCheckpoint(before, List.class.getMethod("add", int.class, Object.class), InjectionPoint.BEFORE);
        CheckpointReached after = runtime.getCheckpoints().get(1);
        assertMethodCheckpoint(after, List.class.getMethod("add", int.class, Object.class), InjectionPoint.AFTER);
    }

    private void assertMethodCheckpoint(CheckpointReached before, Method method, InjectionPoint expectedInjectionPoint) {
        Assert.assertEquals(expectedInjectionPoint, before.checkpoint().injectionPoint());
        MethodCallCheckpoint methodCallCheckpoint = (MethodCallCheckpoint) before.checkpoint();
        Assert.assertEquals(method.getName(), methodCallCheckpoint.methodName());
        Assert.assertEquals(method.getReturnType(), methodCallCheckpoint.returnType());
        Assert.assertArrayEquals(method.getParameterTypes(), methodCallCheckpoint.parameterTypes());
        Assert.assertEquals(method.getDeclaringClass(), methodCallCheckpoint.declaringType());
    }

    public void checkpointWithReturn() {

    }

    public void errorThrown() {

    }

}
