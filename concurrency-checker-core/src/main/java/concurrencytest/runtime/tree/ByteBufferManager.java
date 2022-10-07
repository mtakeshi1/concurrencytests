package concurrencytest.runtime.tree;

import java.nio.ByteBuffer;
import java.util.function.Function;

public interface ByteBufferManager {

    interface RecordEntry {
        int HEADER_LENGTH = 6;

        long offset();

        int size();

        default int contentLength() {
            return size() - HEADER_LENGTH;
        }
    }

    ByteBuffer getExisting(long offset, int size);

    long allocateNewSlice(int size);

    ByteBuffer allocateTemporaryBuffer(int minSize);

    <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function);

    void returnBuffer(ByteBuffer buffer);

}
