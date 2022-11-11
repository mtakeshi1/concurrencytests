package concurrencytest.runtime.tree.offheap;

import concurrencytest.runtime.tree.ByteBufferManagerTest;

public class DirectByteBufferTests extends ByteBufferManagerTest {


    @Override
    protected ByteBufferManager instantiateByteBuffer(int pageSize) {
        return new DirectByteBufferManager(pageSize);
    }
}
