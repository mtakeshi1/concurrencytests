package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.MethodCallCheckpoint;
import concurrencytest.reflection.StaticInitializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public record MethodCallCheckpointImpl(InjectionPoint injectionPoint, String sourceFile,
                                       int lineNumber, Member methodOrConstructor) implements MethodCallCheckpoint {
    @Override
    public String details() {
        return null;
    }

    @Override
    public String methodName() {
        return methodOrConstructor.getName();
    }

    @Override
    public Class<?> declaringType() {
        return methodOrConstructor.getDeclaringClass();
    }

    @Override
    public Class<?>[] parameterTypes() {
        if (methodOrConstructor instanceof Method m) {
            return m.getParameterTypes();
        } else if (methodOrConstructor instanceof Constructor<?> c) {
            return c.getParameterTypes();
        } else {
            return new Class[0];
        }
    }

    @Override
    public Class<?> returnType() {
        if (methodOrConstructor instanceof Method m) {
            return m.getReturnType();
        } else if (methodOrConstructor instanceof Constructor<?> c) {
            return c.getDeclaringClass();
        } else if(methodOrConstructor instanceof StaticInitializer st) {
            return st.getDeclaringClass();
        }
        throw new IllegalArgumentException("Cannot determine return type for: " + methodOrConstructor);
    }
}
