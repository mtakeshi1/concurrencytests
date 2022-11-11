package concurrencytest.runtime.tree;

import concurrencytest.runtime.tree.offheap.AbstractByteBufferManager;
import concurrencytest.runtime.tree.offheap.RegionLock;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ByteBufferManagerImpl extends AbstractByteBufferManager {

    private final ConcurrentMap<Integer, ByteBuffer> allocatedBuffers = new ConcurrentHashMap<>();

    public ByteBufferManagerImpl(int bufferSize) {
        super(bufferSize);
    }

    public ByteBufferManagerImpl() {
        super(8 * 1024);
    }

    @Override
    protected RegionLock allocateNewRegionLock(int page, int offsetInPage, int size) {
        return new InVmRegionLock(page, offsetInPage, size);
    }

    @Override
    protected ByteBuffer getOrAllocateBufferForPage(int page) {
        return allocatedBuffers.computeIfAbsent(page, ignored -> allocatePage(getPageSize()));
    }

    protected ByteBuffer allocatePage(int pageSize) {
        return ByteBuffer.allocate(pageSize);
    }

    @Override
    protected Collection<Integer> knownPages() {
        return allocatedBuffers.keySet();
    }

}
