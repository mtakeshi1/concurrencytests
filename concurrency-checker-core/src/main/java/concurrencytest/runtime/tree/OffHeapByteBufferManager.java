package concurrencytest.runtime.tree;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class OffHeapByteBufferManager implements ByteBufferManager {

    public static final int BUFFER_PAGE_SIZE = 100 * 1024 * 1024;

    private final ConcurrentMap<Integer, ByteBuffer> allocatedBuffers = new ConcurrentHashMap<>();

    private volatile long unusedOffset = 0;

    private int indexForOffset(long offset) {
        return (int) (offset / BUFFER_PAGE_SIZE);
    }


    /**
     * A plain record entry has a header of 6 bytes:
     * - two bytes mark the beggining of a record entry - 0xAAFF
     * - 4 bytes show the size of the record (unsigned int)
     * <p>
     * Include also an end of record two bytes - 0XACAC
     */
    private record PlainRecordEntry(long offset, int totalEntrySize, OffHeapByteBufferManager man) implements RecordEntry {

        static final byte[] HEADER = new byte[]{(byte) 0xAA, (byte) 0xFE};
        static final byte[] FOOTER = new byte[]{(byte) 0xAC, (byte) 0xDC};

        static final int RECORD_ENTRY_PREFFIX_LENGTH = 6;
        static final int RECORD_FOOTER_LENGTH = 2;

        static final int FIXED_PADDING = RECORD_ENTRY_PREFFIX_LENGTH + RECORD_FOOTER_LENGTH;

        int pageIndex() {
            return (int) offset / BUFFER_PAGE_SIZE;
        }

        long begginingOfContentOffset() {
            return offsetInPage() + HEADER_LENGTH;
        }

        int offsetInPage() {
            return (int) (offset % BUFFER_PAGE_SIZE);
        }

        int contentOffsetInPage() {
            return offsetInPage() + HEADER_LENGTH;
        }

        int contentSize() {
            return totalEntrySize - FIXED_PADDING;
        }


        private static void writeToBuffer(ByteBuffer byteBuffer, int contentSize) {
            byteBuffer.put(HEADER);
            byteBuffer.putInt(contentSize);
            byteBuffer.position(contentSize + HEADER_LENGTH);
            byteBuffer.put(FOOTER);
        }

        @Override
        public <T> T readFromRecord(int contentOffset, Function<ByteBuffer, T> bufferFunction) {
            return man.executeLocked(this.begginingOfContentOffset(), contentSize(), bb -> {
                bb.position(contentOffset);
                return bufferFunction.apply(bb);
            }, true);
        }

        @Override
        public <T> T writeToRecord(Function<ByteBuffer, T> bufferFunction) {
            return man.executeLocked(this.offset() + RECORD_ENTRY_PREFFIX_LENGTH, totalEntrySize - FIXED_PADDING, bufferFunction, true);
        }
    }

    interface RegionLock {
        int pageIndex();

        int offsetInPage();

        int size();

        boolean isShared();

        default boolean overlaps(RegionLock other) {
            if (this.pageIndex() != other.pageIndex()) {
                return false;
            }
            if (this.offsetInPage() >= other.offsetInPage()) {
                return other.offsetInPage() + other.size() < this.offsetInPage();
            }
            return this.offsetInPage() + this.size() < other.offsetInPage();
        }

        void lock();

        void unlock();
    }

    @Override
    public RecordEntry getExisting(long offset, int size) {
        if (offset > unusedOffset) {
            advanceToLastUnusedOffset();
            return getExisting(offset, size);
        }
        int page = indexForOffset(offset);
        ByteBuffer buffer = allocatedBuffers.get(page);
        if (buffer == null) {
            throw new IllegalStateException("page number %d not initialized".formatted(page));
        }
        int offsetInPage = (int) (offset % BUFFER_PAGE_SIZE);
        ByteBuffer slice = buffer.slice(offsetInPage, size + PlainRecordEntry.FIXED_PADDING);
        validateRecordEntry(slice, size);
        return new PlainRecordEntry(offset, size + PlainRecordEntry.FIXED_PADDING, this);
    }

    private void validateRecordEntry(ByteBuffer slice, int size) {
        nextByteExpected(slice, PlainRecordEntry.HEADER[0]);
        nextByteExpected(slice, PlainRecordEntry.HEADER[1]);
        int len = slice.getInt();
        if (len != size) {
            throw new IllegalArgumentException("Expected %d length but was %d".formatted(size, len));
        }
        slice.position(size - PlainRecordEntry.RECORD_FOOTER_LENGTH);
        nextByteExpected(slice, PlainRecordEntry.FOOTER[0]);
        nextByteExpected(slice, PlainRecordEntry.FOOTER[1]);
    }

    private void nextByteExpected(ByteBuffer buffer, byte expected) {
        byte b = buffer.get();
        if ((b & 0xff) != PlainRecordEntry.HEADER[0]) {
            throw new IllegalStateException("Expected %x but was %x".formatted(expected & 0xff, b & 0xff));
        }
    }


    private void advanceToLastUnusedOffset() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public RecordEntry allocateNewSlice(int size) {
        throw new RuntimeException("not yet implemented");
    }

    public <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function, boolean readLock) {
        RegionLock regionLock = allocate(offset, size, readLock);
        regionLock.lock();
        try {
            ByteBuffer buffer = allocatedBuffers.get(regionLock.pageIndex());
            if (buffer == null) {
                buffer = initializeNewPage(regionLock.pageIndex());
            }
            ByteBuffer copy = buffer.slice(regionLock.offsetInPage(), size);
            return function.apply(copy);

        } finally {
            regionLock.unlock();
        }
    }

    private ByteBuffer initializeNewPage(int pageIndex) {
        throw new RuntimeException("not yet implemented");
    }

    private RegionLock allocate(long offset, int size, boolean readLock) {
        throw new RuntimeException("not yet implemented");
    }
}
