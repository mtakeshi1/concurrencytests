package concurrencytest.runtime.tree;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ByteBufferManager {

    interface RecordEntry {
        int HEADER_LENGTH = 6;

        long offset();

//        int size();

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

    RecordEntry allocateNewSlice(int size);

    default ByteBuffer allocateTemporaryBuffer(int size) {
        return ByteBuffer.allocate(size);
    }

//    <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function);

    default void returnBuffer(ByteBuffer buffer) {
    }

}
