package concurrencytest.runtime.lock;

public record BlockingResource(LockType lockType, int resourceId, Class<?> resourceType, String sourceCode, int lineNumber) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockingResource that = (BlockingResource) o;

        if (resourceId != that.resourceId) return false;
        return lockType == that.lockType;
    }

    @Override
    public int hashCode() {
        int result = lockType != null ? lockType.hashCode() : 0;
        result = 31 * result + resourceId;
        return result;
    }
}
