package concurrencytest.runtime.tree.offheap;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This ByteBufferManager organizes its entries in pages, represented by files of fixed size in the file system.
 * <p>
 * Each RecordEntry has a fixed header of 2 bytes, followed by the entire record size in 2 bytes, content and them finally 2-bytes footer
 */
public class OffHeapByteBufferManager extends AbstractByteBufferManager implements ByteBufferManager {

    private final File baseFolder;

    public static final int DEFAULT_BUFFER_PAGE_SIZE = 1024 * 1024 * 1024;

    public static final String PAGE_FILE_PREFFIX = "dataFile.";

    public static final Pattern PATTERN = Pattern.compile(Pattern.quote(PAGE_FILE_PREFFIX) + "([0-9]+)$");

    private final ConcurrentMap<Integer, ChannelAndBuffer> allocatedBuffers = new ConcurrentHashMap<>();

    public OffHeapByteBufferManager(File baseFolder, int bufferPageSize) throws IOException {
        super(bufferPageSize);
        if (bufferPageSize % 4096 != 0) {
            throw new IllegalArgumentException("bufferPageSize should be divisable by 4k but was: " + bufferPageSize);
        }
        this.baseFolder = Objects.requireNonNull(baseFolder, "baseFolder cannot be null");
        if (!baseFolder.exists()) {
            Files.createDirectories(baseFolder.toPath());
        } else if (!baseFolder.isDirectory()) {
            throw new IllegalArgumentException("not a directory: %s".formatted(baseFolder.getAbsolutePath()));
        }
    }

    public OffHeapByteBufferManager(File baseFolder) throws IOException {
        this(baseFolder, DEFAULT_BUFFER_PAGE_SIZE);
    }

    @Override
    protected RegionLock allocateNewRegionLock(int page, int offsetInPage, int size) {
        ChannelAndBuffer cnb = getOrAllocateChannelAndBuffer(page);

        return new RegionLock() {

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
            public synchronized <T, E extends Exception> T doWithSharedLock(LockAction<T, E> action) throws E {
                try {
                    FileLock lock = cnb.channel().lock(offsetInPage, size, true);
                    try {
                        return action.execute();
                    } finally {
                        lock.release();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public <T, E extends Exception> T doWithExclusiveLock(LockAction<T, E> action) throws E {
                try {
                    FileLock lock = cnb.channel().lock(offsetInPage, size, false);
                    try {
                        return action.execute();
                    } finally {
                        lock.release();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    @Override
    protected ByteBuffer getOrAllocateBufferForPage(int page) {
        return getOrAllocateChannelAndBuffer(page).buffer();
    }

    private ChannelAndBuffer getOrAllocateChannelAndBuffer(int page) {
        return allocatedBuffers.computeIfAbsent(page, this::allocateChannelAndBuffer);
    }

    @Override
    protected Collection<Integer> knownPages() {
        File[] children = Objects.requireNonNull(baseFolder.listFiles(), "could not list %s".formatted(baseFolder.getAbsolutePath()));
        List<Integer> list = new ArrayList<>();
        for (File file : children) {
            Matcher matcher = PATTERN.matcher(file.getName());
            if (matcher.matches()) {
                list.add(Integer.parseInt(matcher.group(1)));
            }
        }
        return list;
    }

    @Override
    public void close() throws Exception {
        Exception error = null;
        for (var cnb : allocatedBuffers.values()) {
            try {
                cnb.channel().close();
            } catch (Exception e) {
                if (error == null) {
                    error = e;
                } else {
                    error.addSuppressed(e);
                }
            }
        }
        if (error != null) {
            throw error;
        }
    }

    private ChannelAndBuffer allocateChannelAndBuffer(int pageIndex) {
        File file = new File(baseFolder, PAGE_FILE_PREFFIX + pageIndex);
        try {
            if (!file.exists()) {
                var channel = FileChannel.open(file.toPath(), StandardOpenOption.DSYNC, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
                var tmpBuffer = ByteBuffer.allocateDirect(getPageSize() / 4 * 1024);
                tmpBuffer.mark();
                for (int i = 0; i < getPageSize() / tmpBuffer.capacity(); i++) {
                    channel.write(tmpBuffer);
                    tmpBuffer.reset();
                }
                MappedByteBuffer buffer = channel.map(MapMode.READ_WRITE, 0, getPageSize());
                return new ChannelAndBuffer(pageIndex, file, channel, buffer);
            }
            var channel = FileChannel.open(file.toPath(), StandardOpenOption.DSYNC, StandardOpenOption.WRITE, StandardOpenOption.READ);
            MappedByteBuffer buffer = channel.map(MapMode.READ_WRITE, 0, getPageSize());
            return new ChannelAndBuffer(pageIndex, file, channel, buffer);
        } catch (FileAlreadyExistsException e) {
            return allocateChannelAndBuffer(pageIndex);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
