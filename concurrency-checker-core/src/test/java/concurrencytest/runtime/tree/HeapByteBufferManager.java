package concurrencytest.runtime.tree;

import concurrencytest.runtime.tree.offheap.AbstractByteBufferManager;
import concurrencytest.runtime.tree.offheap.RegionLock;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HeapByteBufferManager extends AbstractByteBufferManager {

    private final ConcurrentMap<Integer, ByteBuffer> allocatedBuffers = new ConcurrentHashMap<>();

    public HeapByteBufferManager() {
        super(8 * 1024);
    }

    @Override
    protected RegionLock allocateNewRegionLock(int page, int offsetInPage, int size) {
        return new RegionLock() {

            private final ReadWriteLock lock = new ReentrantReadWriteLock();

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
        };
    }

    @Override
    protected ByteBuffer getOrAllocateBufferForPage(int page) {
        return allocatedBuffers.computeIfAbsent(page, ignored -> ByteBuffer.allocate(getPageSize()));
    }

    @Override
    protected Collection<Integer> knownPages() {
        return allocatedBuffers.keySet();
    }
}
