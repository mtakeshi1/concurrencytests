package concurrencytest.runtime.tree.offheap;

import concurrencytest.runtime.tree.offheap.ByteBufferManager.RecordEntry;
import concurrencytest.util.ByteBufferUtil;
import concurrencytest.util.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 * A plain record entry has a header of 6 bytes:
 * - two bytes mark the beggining of a record entry - 0xAAFF
 * - 4 bytes show the size of the record (unsigned int)
 * <p>
 * Include also an end of record two bytes - 0XACAC
 */
public record PlainRecordEntry(long absoluteOffset, int totalEntrySize, OffHeapByteBufferManager man) implements RecordEntry {
    static byte[] HEADER = new byte[]{(byte) 0xAA, (byte) 0xFE};
    static byte[] FOOTER = new byte[]{(byte) 0xAC, (byte) 0xDC};


    static final int RECORD_ENTRY_PREFFIX_LENGTH = 4;
    static final int RECORD_FOOTER_LENGTH = 2;

    static final int FIXED_PADDING = RECORD_ENTRY_PREFFIX_LENGTH + RECORD_FOOTER_LENGTH;

    public static boolean isValidHeader(byte[] buffer) {
        return Utils.todo();
    }

    public static boolean isValidFooter(byte[] buffer) {
        return Utils.todo();
    }

    int pageIndex() {
        return (int) (absoluteOffset / man.getBufferPageSize());
    }

    long begginingOfContentOffset() {
        return offsetInPage() + HEADER_LENGTH;
    }

    int offsetInPage() {
        return (int) (absoluteOffset % man.getBufferPageSize());
    }

    int contentOffsetInPage() {
        return offsetInPage() + HEADER_LENGTH;
    }

    public int contentSize() {
        return totalEntrySize - FIXED_PADDING;
    }


    private static void writeToBuffer(ByteBuffer byteBuffer, int contentSize) {
        byteBuffer.put(HEADER);
        ByteBufferUtil.writeInt2Bytes(byteBuffer, contentSize + FIXED_PADDING);
        byteBuffer.position(contentSize + HEADER_LENGTH);
        byteBuffer.put(FOOTER);
    }

    @Override
    public <T> T readFromRecord(int contentOffset, Function<ByteBuffer, T> bufferFunction) throws IOException {
        return man.executeLocked(this.absoluteOffset(), totalEntrySize, bb -> {
            bb.position(contentOffset);
            return bufferFunction.apply(bb);
        }, true);
    }

    @Override
    public <T> T writeToRecord(Function<ByteBuffer, T> bufferFunction) throws IOException {
        return man.executeLocked(this.absoluteOffset(), totalEntrySize, bufferFunction, false);
    }
}
