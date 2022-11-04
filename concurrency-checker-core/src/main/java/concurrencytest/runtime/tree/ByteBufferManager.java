package concurrencytest.runtime.tree;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ByteBufferManager {

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

        default <T> T readFromRecord(Function<ByteBuffer, T> bufferFunction) {
            return readFromRecord(0, bufferFunction);
        }

        <T> T readFromRecord(int offset, Function<ByteBuffer, T> bufferFunction);

        <T> T writeToRecord(Function<ByteBuffer, T> bufferFunction);

        default void writeToRecordNoReturn(Consumer<ByteBuffer> bufferConsumer) {
            writeToRecord(bb -> {
                bufferConsumer.accept(bb);
                return null;
            });
        }

        default void overwriteRecord(ByteBuffer buffer) {
            writeToRecordNoReturn(bb -> bb.put(buffer));
        }

    }

    RecordEntry getExisting(long offset, int size);

    RecordEntry getExisting(long offset);

    RecordEntry allocateNewSlice(int size);

    default ByteBuffer allocateTemporaryBuffer(int size) {
        return ByteBuffer.allocate(size);
    }

//    <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function);

    default void returnBuffer(ByteBuffer buffer) {
    }

}
