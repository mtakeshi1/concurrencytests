package concurrencytest.runtime.tree.offheap;

import concurrencytest.runtime.tree.ByteBufferManagerImpl;

import java.nio.ByteBuffer;

public class DirectByteBufferManager extends ByteBufferManagerImpl {

    public DirectByteBufferManager(int bufferSize) {
        super(bufferSize);
    }

    @Override
    protected ByteBuffer allocatePage(int pageSize) {
        return ByteBuffer.allocateDirect(pageSize);
    }
}
