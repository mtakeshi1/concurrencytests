package concurrencytest.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public interface ClassResolver {
    Class<?> resolveName(String className);

    <T> Constructor<T> lookupConstructor(Class<T> declaringClass, Class<?>[] args);

    Method lookupMethod(Class<?> declaringClass, int access, String name, Class<?>[] args, Class<?> returnType);

    Field lookupField(Class<?> ownerType, String name);

    Member resolveMethodOrConstructor(Class<?> callTarget, String methodName, String descriptor);
}
