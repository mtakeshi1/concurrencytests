package concurrencytest.runtime.tree;

import concurrencytest.runtime.tree.offheap.ByteBufferManager;

public class HeapByteBufferManagerTest extends ByteBufferManagerTest {
    @Override
    protected ByteBufferManager instantiateByteBuffer(int pageSize) {
        return new ByteBufferManagerImpl(pageSize);
    }
}
