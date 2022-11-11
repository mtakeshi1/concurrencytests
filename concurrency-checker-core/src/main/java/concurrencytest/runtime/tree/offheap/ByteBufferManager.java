package concurrencytest.runtime.tree.offheap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ByteBufferManager {

    void close() throws Exception;

    int getPageSize();

    <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function, boolean readLock) throws IOException;

    /**
     * A record entry is a piece of a larger chunk that may or may not be split between multiple files. Users don't need to know details of how
     * the file is split.
     */
    interface RecordEntry {

        int MAX_RECORD_LENGTH = 10 * 1024;

        int HEADER_LENGTH = 4;

        int FOOTER_LENGTH = 2;

        long absoluteOffset();

        int contentSize();

        default int recordSize() {
            return contentSize() + HEADER_LENGTH + FOOTER_LENGTH;
        }

        default <T> T readFromRecord(Function<ByteBuffer, T> bufferFunction) throws IOException {
            return readFromRecord(0, bufferFunction);
        }

        default void readFromRecordNoReturn(Consumer<ByteBuffer> bufferFunction) throws IOException {
            readFromRecord(0, b -> {
                bufferFunction.accept(b);
                return null;
            });
        }

        <T> T readFromRecord(int offset, Function<ByteBuffer, T> bufferFunction) throws IOException;

        <T> T writeToRecord(Function<ByteBuffer, T> bufferFunction) throws IOException;

        default void writeToRecordNoReturn(Consumer<ByteBuffer> bufferConsumer) throws IOException {
            writeToRecord(bb -> {
                bufferConsumer.accept(bb);
                return null;
            });
        }

        default void overwriteRecord(ByteBuffer buffer) throws IOException {
            writeToRecordNoReturn(bb -> bb.put(buffer));
        }

    }

    RecordEntry getExisting(long offset, int totalEntrySize);

    RecordEntry getExisting(long offset);

    /**
     * Allocates a new slice of the file with content size equal to the given size
     *
     * @param contentSize size should be less than {@link RecordEntry#MAX_RECORD_LENGTH}
     * @return RecordEntry
     * @throws IOException if an IOException occurs while allocating a new file region or a new file
     */
    RecordEntry allocateNewSlice(int contentSize) throws IOException;

    default ByteBuffer allocateTemporaryBuffer(int size) {
        return ByteBuffer.allocate(size);
    }

//    <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function);

    default void returnBuffer(ByteBuffer buffer) {
    }

}
