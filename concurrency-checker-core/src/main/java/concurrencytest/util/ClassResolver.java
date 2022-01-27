package concurrencytest.util;

public interface ClassResolver {
    Class<?> resolveName(String className);
}
