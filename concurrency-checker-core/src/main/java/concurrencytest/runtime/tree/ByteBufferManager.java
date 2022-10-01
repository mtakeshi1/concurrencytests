package concurrencytest.runtime.tree;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface ByteBufferManager {

    record BufferWithOffset(ByteBuffer buffer, long offset) {}

    ByteBuffer getExisting(long offset, int size);

    long allocateNewSlice(int size);

    ByteBuffer allocateTemporaryBuffer(int minSize);

    void executeLocked(long offset, int size, Consumer<ByteBuffer> function);

    void returnBuffer(ByteBuffer buffer);

}
