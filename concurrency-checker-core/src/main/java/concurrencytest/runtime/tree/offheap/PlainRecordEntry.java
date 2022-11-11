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
public record PlainRecordEntry(long absoluteOffset, int totalEntrySize, ByteBufferManager man) implements RecordEntry {
    static byte[] HEADER = new byte[]{(byte) 0xAA, (byte) 0xFE};
    static byte[] FOOTER = new byte[]{(byte) 0xAC, (byte) 0xDC};


    static final int RECORD_ENTRY_PREFFIX_LENGTH = 4;
    static final int RECORD_FOOTER_LENGTH = 2;

    static final int FIXED_PADDING = RECORD_ENTRY_PREFFIX_LENGTH + RECORD_FOOTER_LENGTH;

    public static boolean isValidHeader(byte[] buffer) {
        return buffer.length >= 2 && buffer[0] == HEADER[0] && buffer[1] == HEADER[1];
    }

    public static boolean isValidFooter(byte[] buffer) {
        return buffer.length >= 2 && buffer[0] == FOOTER[0] && buffer[1] == FOOTER[1];
    }

    int pageIndex() {
        return (int) (absoluteOffset / man.getPageSize());
    }

    long begginingOfContentOffset() {
        return offsetInPage() + HEADER_LENGTH;
    }

    int offsetInPage() {
        return (int) (absoluteOffset % man.getPageSize());
    }

    int contentOffsetInPage() {
        return offsetInPage() + HEADER_LENGTH;
    }

    public int contentSize() {
        return totalEntrySize - FIXED_PADDING;
    }

    @Override
    public <T> T readFromRecord(int contentOffset, Function<ByteBuffer, T> bufferFunction) throws IOException {
        return man.executeLocked(this.absoluteOffset() + HEADER_LENGTH, contentSize(), bufferFunction, true);
    }

    @Override
    public <T> T writeToRecord(Function<ByteBuffer, T> bufferFunction) throws IOException {
        return man.executeLocked(this.absoluteOffset() + HEADER_LENGTH, contentSize(), bufferFunction, false);
    }
}
