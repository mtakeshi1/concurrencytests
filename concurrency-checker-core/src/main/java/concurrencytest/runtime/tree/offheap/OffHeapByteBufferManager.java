package concurrencytest.runtime.tree.offheap;

import concurrencytest.util.ByteBufferUtil;
import concurrencytest.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This ByteBufferManager organizes its entries in pages, represented by files of fixed size in the file system.
 * <p>
 * Each RecordEntry has a fixed header of 2 bytes, followed by the entire record size in 2 bytes, content and them finally 2-bytes footer
 */
public class OffHeapByteBufferManager implements ByteBufferManager {

    private final File baseFolder;

    public static final int DEFAULT_BUFFER_PAGE_SIZE = 1024 * 1024 * 1024;

    public static final String PAGE_FILE_PREFFIX = "dataFile.";

    public static final Pattern PATTERN = Pattern.compile(Pattern.quote(PAGE_FILE_PREFFIX) + "([0-9]+)$");

    private final int bufferPageSize; // 1gb

    private final ConcurrentMap<Integer, ChannelAndBuffer> allocatedBuffers = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, PageLock> pageLockMap = new ConcurrentHashMap<>();

    private final AtomicLong unusedOffset = new AtomicLong();

    public OffHeapByteBufferManager(File baseFolder, int bufferPageSize) throws IOException {
        if (bufferPageSize % 4096 != 0) {
            throw new IllegalArgumentException("bufferPageSize should be divisable by 4k but was: " + bufferPageSize);
        }
        this.baseFolder = Objects.requireNonNull(baseFolder, "baseFolder cannot be null");
        this.bufferPageSize = bufferPageSize;
        if (!baseFolder.exists()) {
            Files.createDirectories(baseFolder.toPath());
        } else if (!baseFolder.isDirectory()) {
            throw new IllegalArgumentException("not a directory: %s".formatted(baseFolder.getAbsolutePath()));
        }
        File[] children = Objects.requireNonNull(baseFolder.listFiles((dir, name) -> PATTERN.matcher(name).matches()), "could not list %s".formatted(baseFolder.getAbsolutePath()));
        if (children.length == 0) {
            ChannelAndBuffer buffer = getOrInitializeBlankPage(0);
            if (!buffer.file().exists()) {
                throw new IOException("could not initialize page 0");
            }
            children = new File[]{buffer.file()};
        }
        for (var file : children) {
            Matcher m = PATTERN.matcher(file.getName());
            if (m.matches()) {
                int pageIndex = Integer.parseInt(m.group(1));
                allocatedBuffers.put(pageIndex, allocateChannelAndBuffer(pageIndex, file));
            }
        }
        advanceToLastUnusedOffset();
    }

    public OffHeapByteBufferManager(File baseFolder) throws IOException {
        this(baseFolder, DEFAULT_BUFFER_PAGE_SIZE);
    }

    public int getBufferPageSize() {
        return bufferPageSize;
    }

    private int pageNumberForAbsoluteOffset(long offset) {
        return (int) (offset / bufferPageSize);
    }

    interface RegionLock {
        int pageIndex();

        int offsetInPage();

        int size();

        boolean shared();

        default boolean overlaps(RegionLock other) {
            if (this.pageIndex() != other.pageIndex()) {
                return false;
            }
            if (this.offsetInPage() >= other.offsetInPage()) {
                return other.offsetInPage() + other.size() < this.offsetInPage();
            }
            return this.offsetInPage() + this.size() < other.offsetInPage();
        }

        void lock() throws IOException;

        void unlock() throws IOException;
    }

    record PageLock(int pageIndex, ReadWriteLock lock, FileChannel channel) {
    }

    @Override
    public RecordEntry getExisting(long offset, int size) {
        if (offset > unusedOffset.get()) {
            FileChannel channel = null;
            advanceToLastUnusedOffset();
            return getExisting(offset, size);
        }
        int page = pageNumberForAbsoluteOffset(offset);
        ByteBuffer buffer = allocatedBuffers.computeIfAbsent(page, this::allocateChannelAndbuffer).buffer();
        int offsetInPage = (int) (offset % bufferPageSize);
        ByteBuffer slice = buffer.slice(offsetInPage, size + PlainRecordEntry.FIXED_PADDING);
        validateRecordEntry(slice, size);
        return new PlainRecordEntry(offset, size + PlainRecordEntry.FIXED_PADDING, this);
    }

    @Override
    public RecordEntry getExisting(long offset) {
        if (offset > unusedOffset.get()) {
            advanceToLastUnusedOffset();
            return getExisting(offset);
        }
        int page = pageNumberForAbsoluteOffset(offset);
        ByteBuffer buffer = allocatedBuffers.computeIfAbsent(page, this::allocateChannelAndbuffer).buffer();
        int offsetInPage = (int) (offset % bufferPageSize);
        byte[] sizeBytes = new byte[2];
        buffer.get(offsetInPage + 2, sizeBytes);
        int size = intFrom2Bytes(sizeBytes);
        ByteBuffer slice = buffer.slice(offsetInPage, size + PlainRecordEntry.FIXED_PADDING);
        validateRecordEntry(slice, size);
        return new PlainRecordEntry(offset, size + PlainRecordEntry.FIXED_PADDING, this);
    }

    private static int intFrom2Bytes(byte[] sizeBytes) {
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

    public <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function, boolean readLock) throws IOException {
        RegionLock regionLock = allocateRegionLock(offset, size, readLock);
        regionLock.lock();
        try {
            ByteBuffer buffer = allocatedBuffers.computeIfAbsent(regionLock.pageIndex(), this::allocateChannelAndbuffer).buffer();
            ByteBuffer copy = buffer.slice(regionLock.offsetInPage(), size);
            return function.apply(copy);
        } finally {
            regionLock.unlock();
        }
    }

    protected File findLastPage() {
        File[] files = baseFolder.listFiles((dir, name) -> PATTERN.matcher(name).matches());
        Comparator<File> cmp = (a, b) -> {
            Matcher aMatcher = PATTERN.matcher(a.getName());
            Matcher bMatcher = PATTERN.matcher(b.getName());
            if (aMatcher.matches() && bMatcher.matches()) {
                return Integer.compare(Integer.parseInt(aMatcher.group(1)), Integer.parseInt(bMatcher.group(1)));
            }
            throw new IllegalArgumentException("Did not expect file with name: %s or file with name: %s".formatted(a.getName(), b.getName()));
        };
        Arrays.sort(Objects.requireNonNull(files, "listFiles on folder: %s returned null. Maybe it was deleted?".formatted(baseFolder.getAbsolutePath())), cmp);
        if (files.length == 0) {
            throw new IllegalArgumentException("No file has been found on directory: " + baseFolder.getAbsolutePath());
        }
        return files[files.length - 1];
    }

    /**
     * Try to seek the backing file or check for newer files, maybe?
     */
    private void advanceToLastUnusedOffset() {
        long expectedLastOffset = unusedOffset.get();
        File[] files = baseFolder.listFiles();
        int maxPage = 0;
        ChannelAndBuffer chanAndBuf = null;
        for (var file : Objects.requireNonNull(files, "listFiles on folder: %s returned null. Maybe it was deleted?".formatted(baseFolder.getAbsolutePath()))) {
            Matcher m = PATTERN.matcher(file.getName());
            if (m.matches()) {
                int pageNumber = Integer.parseInt(m.group(1));
                var cnb = allocatedBuffers.computeIfAbsent(pageNumber, num -> allocateChannelAndBuffer(pageNumber, file));
                if (pageNumber > maxPage || chanAndBuf == null) {
                    chanAndBuf = cnb;
                    maxPage = pageNumber;
                }
            }
        }
        long localMaxOffset = ((long) maxPage) * bufferPageSize;
        ByteBuffer bbuffer = Objects.requireNonNull(chanAndBuf, "did not find any buffer on page: " + maxPage).buffer();
        int pageOffset = 0;
        while (pageOffset < bufferPageSize) {
            RecordEntry recordEntry = readFrom(bbuffer, pageOffset, maxPage);
            if (recordEntry == null) {
                if (unusedOffset.compareAndSet(expectedLastOffset, localMaxOffset + pageOffset)) {
                    return;
                }
                break; // clean retry
            } else {
                pageOffset += recordEntry.recordSize();
            }
        }
        // we didn't find an empty space, we recursively try again
        advanceToLastUnusedOffset();
    }

    /**
     * Read the entry from the buffer starting at the given offset, if present.
     * Otherwise, return null;
     *
     * @param bbuffer the ByteBuffer containing the record
     * @param offset  the offset to read the RecordEntry from
     * @return the RecordEntry or null if its incomplete and/or end of buffer
     */
    public RecordEntry readFrom(ByteBuffer bbuffer, int offset, int pageNumber) {
        if (bbuffer.remaining() > offset + RecordEntry.HEADER_LENGTH + RecordEntry.FOOTER_LENGTH + 1) {
            byte[] buffer = new byte[2];
            bbuffer.get(offset, buffer);
            if (PlainRecordEntry.isValidHeader(buffer)) {
                // we know somebody is writing here. We wait until
                while (true) {
                    bbuffer.get(offset + 2, buffer);
                    int size = intFrom2Bytes(buffer);
                    if (size == 0) {
                        Thread.yield();
                        continue;
                    }
                    bbuffer.get(offset + size - 2, buffer);
                    if (PlainRecordEntry.isValidFooter(buffer)) {
                        return new PlainRecordEntry((long) pageNumber * bufferPageSize + offset, size, this);
                    }
                }
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Allocates a new slice with the given content size
     *
     * @param size the size. Must be less than {@link RecordEntry#MAX_RECORD_LENGTH}
     * @return freshly empty RecordEntry
     */
    @Override
    public RecordEntry allocateNewSlice(int size) throws IOException {
        advanceToLastUnusedOffset();
        long offset = unusedOffset.get();
        int page = pageNumberForAbsoluteOffset(offset);
        int offsetInPage = offsetInPage(offset, page);
        while (offsetInPage > bufferPageSize) {
            page++;
            offsetInPage = offsetInPage(offset, page);
        }
        offset = ((long) page) * bufferPageSize + offsetInPage;
        ChannelAndBuffer buffer = getOrInitializeBlankPage(size);
        RegionLock lock = allocateRegionLock(offset, size, false);
        lock.lock();
        try {
            byte[] tmp = new byte[2];
            buffer.buffer().get(offsetInPage, tmp);
            if (!PlainRecordEntry.isValidHeader(tmp)) {
                buffer.buffer().put(offsetInPage, PlainRecordEntry.HEADER);
                buffer.buffer().put(offsetInPage + 2, putInt2Bytes(tmp, size));
                buffer.buffer().put(offsetInPage + size - RecordEntry.FOOTER_LENGTH, PlainRecordEntry.FOOTER);
                updateUnusedOffsetIfNecessary(offset + size);
                return new PlainRecordEntry(offset, size, this);
            }
        } finally {
            lock.unlock();
        }
        Thread.yield();
        return allocateNewSlice(size);
    }

    private static byte[] putInt2Bytes(byte[] tmp, int value) {
        tmp[0] = (byte) ((value >>> 8) & 0xff);
        tmp[1] = (byte) (value & 0xff);
        return tmp;
    }

    private void updateUnusedOffsetIfNecessary(long offset) {
        while (true) {
            long old = unusedOffset.get();
            if (old < offset) {
                if (unusedOffset.compareAndSet(old, offset)) break;
            } else break;
        }
    }

    public int offsetInPage(long offset, int page) {
        return (int) (offset - page * bufferPageSize);
    }

    private ChannelAndBuffer allocateChannelAndBuffer(int pageIndex, File file) {
        try {
            var channel = FileChannel.open(file.toPath(), StandardOpenOption.DSYNC, StandardOpenOption.WRITE, StandardOpenOption.READ);
            MappedByteBuffer buffer = channel.map(MapMode.READ_WRITE, 0, bufferPageSize);
            return new ChannelAndBuffer(pageIndex, file, channel, buffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Forces initialization of a blank page if the file doesn't exist, or allocates the ChannelAndBuffer
     *
     * @param pageIndex the corresponding page number
     * @return ChannelAndBuffer never null
     */
    private ChannelAndBuffer getOrInitializeBlankPage(int pageIndex) {
        ChannelAndBuffer present = allocatedBuffers.get(pageIndex);
        if (present != null) {
            return present;
        }
        File file = new File(PAGE_FILE_PREFFIX + pageIndex);
        try {
            if (file.exists()) {
                var channel = FileChannel.open(file.toPath(), StandardOpenOption.DSYNC, StandardOpenOption.WRITE, StandardOpenOption.READ);
                MappedByteBuffer buffer = channel.map(MapMode.READ_WRITE, 0, bufferPageSize);
                return new ChannelAndBuffer(pageIndex, file, channel, buffer);
            } else {
                var channel = FileChannel.open(file.toPath(), StandardOpenOption.DSYNC, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
                var tmpBuffer = ByteBuffer.allocateDirect(bufferPageSize / 1024);
                tmpBuffer.mark();
                for (int i = 0; i < bufferPageSize / tmpBuffer.remaining(); i++) {
                    channel.write(tmpBuffer);
                    tmpBuffer.reset();
                }
                MappedByteBuffer buffer = channel.map(MapMode.READ_WRITE, 0, bufferPageSize);
                return new ChannelAndBuffer(pageIndex, file, channel, buffer);
            }
        } catch (FileAlreadyExistsException e) {
            // we will try again, as someone else beat me to this
            return getOrInitializeBlankPage(pageIndex);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Initializes a new page file, with size {@link OffHeapByteBufferManager#bufferPageSize}
     *
     * @param pageIndex the index
     * @return MappedByteBuffer for the entire file
     */
    private ChannelAndBuffer allocateChannelAndbuffer(int pageIndex) {
        File file = new File(PAGE_FILE_PREFFIX + pageIndex);
        return allocateChannelAndBuffer(pageIndex, file);
    }

    private RegionLock allocateRegionLock(long offset, int size, boolean readLock) {
        int page = pageNumberForAbsoluteOffset(offset);
        int end = pageNumberForAbsoluteOffset(offset + size);
        if (page != end) {
            throw new RuntimeException("region lock would exceed a single page =(");
        }
        int offsetInPage = offsetInPage(offset, page);
        ChannelAndBuffer cnb = getOrInitializeBlankPage(page);
        var existing = pageLockMap.computeIfAbsent(page, ignored -> new PageLock(page, new ReentrantReadWriteLock(), cnb.channel()));

        return new RegionLock() {

            FileLock regionLock = null;

            @Override
            public int pageIndex() {
                return page;
            }

            @Override
            public int offsetInPage() {
                return offsetInPage;
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            public boolean shared() {
                return readLock;
            }

            @Override
            public void lock() throws IOException {
                if (regionLock == null) {
                    throw new IllegalStateException("this region is already locked!");
                }
                if (readLock) {
                    existing.lock().readLock().lock();
                } else {
                    existing.lock().writeLock().lock();
                }
                try {
                    regionLock = existing.channel.lock(offsetInPage, size, readLock);
                } catch (IOException e) {
                    unlock();
                    throw e;
                }
            }

            @Override
            public void unlock() throws IOException {
                if (readLock) {
                    existing.lock().readLock().unlock();
                } else {
                    existing.lock().writeLock().unlock();
                }
                if (regionLock != null) {
                    regionLock.release();
                }
            }
        };
    }
}
