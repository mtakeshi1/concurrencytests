package concurrencytest.runtime.tree;

import concurrencytest.util.ByteBufferUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits the backing file into RecordEntry's
 * <p>
 * Each RecordEntry has a fixed header of 2 bytes, followed by the entire record size in 2 bytes, content and them finally 2-bytes footer
 */
public class OffHeapByteBufferManager implements ByteBufferManager {

    private final File baseFolder;

    private static final Pattern PATTERN = Pattern.compile("dataFile\\.([0-9]+)$");

    public static final long BUFFER_PAGE_SIZE = 2L * 1024 * 1024 * 1024; // 2gb

    public OffHeapByteBufferManager(File baseFolder) throws IOException {
        this.baseFolder = baseFolder;
        if (!baseFolder.exists()) {
            Files.createDirectories(baseFolder.toPath());
        } else if (!baseFolder.isDirectory()) {
            throw new IllegalArgumentException("not a directory: %s".formatted(baseFolder.getAbsolutePath()));
        }
        File[] children = baseFolder.listFiles((dir, name) -> PATTERN.matcher(name).matches());
        if (children == null || children.length == 0) {
            File file = new File(baseFolder, "dataFile.0");
            try (var fout = new FileOutputStream(file)) {
                byte[] buffer = new byte[(int) (BUFFER_PAGE_SIZE / 1024)];
                for (int i = 0; i < BUFFER_PAGE_SIZE / buffer.length; i++) {
                    fout.write(buffer);
                }
            }
            children = new File[]{file};
            initializePage(0);
            if (!file.exists()) {
                throw new IOException("could not initialize page 0");
            }
        }
        for (var file : children) {
            Matcher m = PATTERN.matcher(file.getName());
            if (m.matches()) {
                int pageIndex = Integer.parseInt(m.group(1));

            }
        }
    }

    private record ChannelAndBuffer(File file, FileChannel channel, ByteBuffer buffer) {
    }

    private final ConcurrentMap<Integer, ChannelAndBuffer> allocatedBuffers = new ConcurrentHashMap<>();

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
    private record PlainRecordEntry(long absoluteOffset, int totalEntrySize, OffHeapByteBufferManager man) implements RecordEntry {

        static final byte[] HEADER = new byte[]{(byte) 0xAA, (byte) 0xFE};
        static final byte[] FOOTER = new byte[]{(byte) 0xAC, (byte) 0xDC};

        static final int RECORD_ENTRY_PREFFIX_LENGTH = 4;
        static final int RECORD_FOOTER_LENGTH = 2;

        static final int FIXED_PADDING = RECORD_ENTRY_PREFFIX_LENGTH + RECORD_FOOTER_LENGTH;

        int pageIndex() {
            return (int) (absoluteOffset / BUFFER_PAGE_SIZE);
        }

        long begginingOfContentOffset() {
            return offsetInPage() + HEADER_LENGTH;
        }

        int offsetInPage() {
            return (int) (absoluteOffset % BUFFER_PAGE_SIZE);
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
        public <T> T readFromRecord(int contentOffset, Function<ByteBuffer, T> bufferFunction) {
            return man.executeLocked(this.begginingOfContentOffset(), contentSize(), bb -> {
                bb.position(contentOffset);
                return bufferFunction.apply(bb);
            }, true);
        }

        @Override
        public <T> T writeToRecord(Function<ByteBuffer, T> bufferFunction) {
            return man.executeLocked(this.absoluteOffset() + RECORD_ENTRY_PREFFIX_LENGTH, totalEntrySize - FIXED_PADDING, bufferFunction, false);
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
        ByteBuffer buffer = allocatedBuffers.computeIfAbsent(page, this::initializePage).buffer();
        int offsetInPage = (int) (offset % BUFFER_PAGE_SIZE);
        ByteBuffer slice = buffer.slice(offsetInPage, size + PlainRecordEntry.FIXED_PADDING);
        validateRecordEntry(slice, size);
        return new PlainRecordEntry(offset, size + PlainRecordEntry.FIXED_PADDING, this);
    }

    @Override
    public RecordEntry getExisting(long offset) {
        if (offset > unusedOffset) {
            advanceToLastUnusedOffset();
            return getExisting(offset);
        }
        int page = indexForOffset(offset);
        ByteBuffer buffer = allocatedBuffers.computeIfAbsent(page, this::initializePage).buffer();
        int offsetInPage = (int) (offset % BUFFER_PAGE_SIZE);
        byte[] sizeBytes = new byte[2];
        buffer.get(offsetInPage + 2, sizeBytes);
        int size = fromBytes(sizeBytes);
        ByteBuffer slice = buffer.slice(offsetInPage, size + PlainRecordEntry.FIXED_PADDING);
        validateRecordEntry(slice, size);
        return new PlainRecordEntry(offset, size + PlainRecordEntry.FIXED_PADDING, this);
    }

    private int fromBytes(byte[] sizeBytes) {
        return (sizeBytes[0] & 0xff) << 8 | (sizeBytes[1] & 0xff);
    }

    /**
     * verifies that the slice of the bytebuffer is valid
     *
     * @param slice the slice containing the record
     * @param size  the expected size of the record
     */
    private void validateRecordEntry(ByteBuffer slice, int size) {
        nextByteExpected(slice, PlainRecordEntry.HEADER[0]);
        nextByteExpected(slice, PlainRecordEntry.HEADER[1]);
        int len = ByteBufferUtil.readInt2Bytes(slice);
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


    public <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function, boolean readLock) {
        RegionLock regionLock = allocateRegionLock(offset, size, readLock);
        regionLock.lock();
        try {
            ByteBuffer buffer = allocatedBuffers.computeIfAbsent(regionLock.pageIndex(), this::initializePage).buffer();
            ByteBuffer copy = buffer.slice(regionLock.offsetInPage(), size);
            return function.apply(copy);
        } finally {
            regionLock.unlock();
        }
    }

    /**
     * Try to seek the backing file or check for newer files, maybe?
     */
    private void advanceToLastUnusedOffset() {
        throw new RuntimeException("not yet implemented");
    }

    /**
     * Allocates a new slice with the given content size
     *
     * @param size the size. Must be less than {@link RecordEntry#MAX_RECORD_LENGTH}
     * @return freshly empty RecordEntry
     */
    @Override
    public RecordEntry allocateNewSlice(int size) {

        throw new RuntimeException("not yet implemented");
    }

    /**
     * Initializes a new page file, with size {@link OffHeapByteBufferManager#BUFFER_PAGE_SIZE}
     *
     * @param pageIndex the index
     * @return MappedByteBuffer for the entire file
     */
    private ChannelAndBuffer initializePage(int pageIndex) {
        throw new RuntimeException("not yet implemented");
    }

    private RegionLock allocateRegionLock(long offset, int size, boolean readLock) {
        throw new RuntimeException("not yet implemented");
    }
}
