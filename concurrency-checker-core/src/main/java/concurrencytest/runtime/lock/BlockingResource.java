package concurrencytest.runtime.lock;

public record BlockingResource(LockType lockType, int resourceId, Class<?> resourceType, String sourceCode, int lineNumber) {
}
