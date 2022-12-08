package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public record MethodCallCheckpointImpl(InjectionPoint injectionPoint, String sourceFile,
                                       int lineNumber, String methodName, Class<?> declaringType, Class<?>[] parameterTypes,
                                       Class<?> returnType) implements MethodCallCheckpointDescription {

    public MethodCallCheckpointImpl(InjectionPoint injectionPoint, String sourceFile,
                                    int lineNumber, Member methodOrConstructor) {
        this(injectionPoint, sourceFile, lineNumber, methodOrConstructor.getName(), methodOrConstructor.getDeclaringClass(), findParameterTypes(methodOrConstructor), findReturnType(methodOrConstructor));
    }

    private static Class<?> findReturnType(Member methodOrConstructor) {
        if (methodOrConstructor instanceof Constructor<?>) {
            return methodOrConstructor.getDeclaringClass();
        } else if (methodOrConstructor instanceof Method method) {
            return method.getReturnType();
        }
        throw new IllegalArgumentException("not a method or constructor: %s".formatted(methodOrConstructor));
    }

    private static Class<?>[] findParameterTypes(Member methodOrConstructor) {
        if (methodOrConstructor instanceof Executable ctor) {
            return ctor.getParameterTypes();
        }
        throw new IllegalArgumentException("not a method or constructor: %s".formatted(methodOrConstructor));
    }

    @Override
    public String toString() {
        return "%s %s.%s (%s:%d)".formatted(injectionPoint, declaringType.getName(), methodName, sourceFile, lineNumber);
    }

    @Override
    public String details() {
        return null;
    }

}
