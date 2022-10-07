package concurrencytest.runtime.tree;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class OffHeapByteBufferManager implements ByteBufferManager {

    public static final int BUFFER_LENGTH = 100 * 1024 * 1024;

    private final ConcurrentMap<Integer, ByteBuffer> allocatedBuffers = new ConcurrentHashMap<>();

    private volatile long unsedOffset = 0;

    private int indexForOffset(long offset) {
        return (int) (offset / BUFFER_LENGTH);
    }

    @Override
    public ByteBuffer getExisting(long offset, int size) {
        int page = indexForOffset(offset);
        ByteBuffer buffer = allocatedBuffers.get(page);
        if (buffer == null) {
            throw new IllegalStateException("Page not allocated for offset %d".formatted(offset));
        }
        return buffer;
    }

    @Override
    public long allocateNewSlice(int size) {
        boolean r = executeLocked(unsedOffset, size, bb -> {
            if(isEmpty(bb)) {

            }
            return true;
        });
        return 0;
    }

    private boolean isEmpty(ByteBuffer buffer) {
        return false;
    }

    @Override
    public ByteBuffer allocateTemporaryBuffer(int minSize) {
        return null;
    }

    @Override
    public <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function) {
        return null;
    }

    @Override
    public void returnBuffer(ByteBuffer buffer) {

    }
}
