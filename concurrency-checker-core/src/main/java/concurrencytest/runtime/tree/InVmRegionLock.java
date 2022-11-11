package concurrencytest.runtime.tree;

import concurrencytest.runtime.tree.offheap.RegionLock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InVmRegionLock implements RegionLock {

    private final ReadWriteLock lock;
    private final int page;
    private final int offsetInPage;
    private final int size;

    public InVmRegionLock(int page, int offsetInPage, int size) {
        this.page = page;
        this.offsetInPage = offsetInPage;
        this.size = size;
        lock = new ReentrantReadWriteLock();
    }

    @Override
    public int pageIndex() {
        return page;
    }

    @Override
    public int offsetInPage() {
        return offsetInPage;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public <T, E extends Exception> T doWithSharedLock(LockAction<T, E> action) throws E {
        lock.readLock().lock();
        try {
            return action.execute();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T, E extends Exception> T doWithExclusiveLock(LockAction<T, E> action) throws E {
        lock.writeLock().lock();
        try {
            return action.execute();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
