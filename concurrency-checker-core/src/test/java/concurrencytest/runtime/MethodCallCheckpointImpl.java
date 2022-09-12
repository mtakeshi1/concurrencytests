package concurrencytest.runtime;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.MethodCallCheckpoint;

import java.lang.reflect.Method;

public record MethodCallCheckpointImpl(int checkpointId, InjectionPoint injectionPoint, String sourceFile,
                                       int lineNumber,
                                       Method method) implements MethodCallCheckpoint {
    @Override
    public String details() {
        return null;
    }

    @Override
    public String methodName() {
        return method.getName();
    }

    @Override
    public Class<?> declaringType() {
        return method.getDeclaringClass();
    }

    @Override
    public Class<?>[] parameterTypes() {
        return method.getParameterTypes();
    }

    @Override
    public Class<?> returnType() {
        return method.getReturnType();
    }
}
