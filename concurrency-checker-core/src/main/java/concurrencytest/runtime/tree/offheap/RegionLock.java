package concurrencytest.runtime.tree.offheap;

public interface RegionLock {
    int pageIndex();

    int offsetInPage();

    int size();

    default boolean overlaps(RegionLock other) {
        if (this.pageIndex() != other.pageIndex()) {
            return false;
        }
        if (this.offsetInPage() >= other.offsetInPage()) {
            return other.offsetInPage() + other.size() < this.offsetInPage();
        }
        return this.offsetInPage() + this.size() < other.offsetInPage();
    }

    interface LockAction<T, E extends Exception> {
        T execute() throws E;
    }

    <T, E extends Exception> T doWithSharedLock(LockAction<T, E> action) throws E;

    <T, E extends Exception> T doWithExclusiveLock(LockAction<T, E> action) throws E;

//
//    void lock(boolean readLock) throws IOException;
//
//    void unlock(boolean readLock) throws IOException;
}
