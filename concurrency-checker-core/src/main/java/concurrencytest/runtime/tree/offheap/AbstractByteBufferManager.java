package concurrencytest.runtime.tree.offheap;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public abstract class AbstractByteBufferManager implements ByteBufferManager {

    private final ConcurrentMap<Long, SoftReference<RegionLock>> allocatedLocks = new ConcurrentHashMap<>();
    private final int pageSize;
    private final AtomicLong freeOffset = new AtomicLong();

    public AbstractByteBufferManager(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public static void putInt2Bytes(ByteBuffer buffer, int value) {
        byte b0 = (byte) ((value >>> 8) & 0xff);
        buffer.put(b0);
        byte b1 = (byte) (value & 0xff);
        buffer.put(b1);
    }

    public static byte[] putInt2Bytes(byte[] tmp, int value) {
        tmp[0] = (byte) ((value >>> 8) & 0xff);
        tmp[1] = (byte) (value & 0xff);
        return tmp;
    }

    @Override
    public int numberOfPages() {
        return knownPages().size();
    }

    @Override
    public long knownFreeOffset() {
        return freeOffset.get();
    }

    public static int intFrom2Bytes(byte[] sizeBytes) {
        return intFrom2Bytes(sizeBytes, 0);
    }

    public static int intFrom2Bytes(byte[] sizeBytes, int offset) {
        return (sizeBytes[offset] & 0xff) << 8 | (sizeBytes[offset + 1] & 0xff);
    }

    @Override
    public RecordEntry getExisting(long offset) {
        int offsetInPage = (int) (offset % pageSize);
        int page = offsetInPage / pageSize;
        ByteBuffer buffer = getOrAllocateBufferForPage(page);
        byte[] maybeHeader = new byte[RecordEntry.HEADER_LENGTH];
        buffer.get(offsetInPage, maybeHeader);
        if (!PlainRecordEntry.isValidHeader(maybeHeader)) {
            throw new IllegalStateException("expected proper header on page %d starting on offset: %d".formatted(page, offsetInPage));
        }
        int expectedContentSize = intFrom2Bytes(maybeHeader, 2);
        return getExisting(offset, expectedContentSize + RecordEntry.HEADER_LENGTH + RecordEntry.FOOTER_LENGTH);
    }

    @Override
    public RecordEntry getExisting(long offset, int totalEntrySize) {
        int offsetInPage = (int) (offset % pageSize);
        int page = offsetInPage / pageSize;
        ByteBuffer buffer = getOrAllocateBufferForPage(page);
        byte[] maybeHeader = new byte[RecordEntry.HEADER_LENGTH];
        buffer.get(offsetInPage, maybeHeader);
        if (!PlainRecordEntry.isValidHeader(maybeHeader)) {
            throw new IllegalStateException("expected proper header on page %d starting at offset: %d".formatted(page, offsetInPage));
        }
        int expectedContentSize = intFrom2Bytes(maybeHeader, 2);
        if (expectedContentSize != totalEntrySize) {
            throw new IllegalStateException("record size does not match: expected: %d found on ByteBuffer: %d".formatted(totalEntrySize, expectedContentSize));
        }
        byte[] maybeFooter = new byte[RecordEntry.FOOTER_LENGTH];
        buffer.get(offsetInPage + RecordEntry.HEADER_LENGTH + expectedContentSize, maybeFooter);
        if (!PlainRecordEntry.isValidFooter(maybeFooter)) {
            throw new IllegalStateException("expected proper footer on page %d starting at offset: %d".formatted(page, offsetInPage));
        }
        return new PlainRecordEntry(offset, totalEntrySize, this);
    }

    @Override
    public RecordEntry allocateNewSlice(int contentSize) {
        if (contentSize > RecordEntry.MAX_RECORD_LENGTH) {
            throw new IllegalArgumentException("requested content size (%d) > MAX_RECORD_LENGTH".formatted(contentSize));
        }
        if (contentSize + RecordEntry.HEADER_LENGTH + RecordEntry.FOOTER_LENGTH > pageSize) {
            throw new IllegalArgumentException("requested content size (%d) > usefull pageSize (%d)".formatted(contentSize, pageSize - RecordEntry.HEADER_LENGTH - RecordEntry.FOOTER_LENGTH));
        }
        if (contentSize <= 0) {
            throw new IllegalArgumentException("requested content size cannot be <= 0");
        }
        long freeOffset = getLastUnusedOffset();
        int offsetInPage = (int) (freeOffset % pageSize);
        int page = offsetInPage / pageSize;
        int totalEntrySize = contentSize + RecordEntry.HEADER_LENGTH + RecordEntry.FOOTER_LENGTH;
        if (offsetInPage + totalEntrySize >= pageSize) {
            page++;
            freeOffset = (long) page * pageSize;
        }
        if (executeLocked(freeOffset, totalEntrySize, buffer -> initializeNewRecordEntry(contentSize, buffer), false)) {
            updateFreeOffset(freeOffset + totalEntrySize);
            return new PlainRecordEntry(freeOffset, totalEntrySize, this);
        }
        rescanUnusedOffset();
        return allocateNewSlice(contentSize);
    }

    private void rescanUnusedOffset() {
        int maxPage = knownPages().stream().max(Integer::compare).orElseThrow();
        long pageoffset = (long) maxPage * pageSize;
        byte[] maybeHeader = new byte[RecordEntry.HEADER_LENGTH];
        byte[] maybeFooter = new byte[RecordEntry.FOOTER_LENGTH];
        int currentOffset = 0;
        ByteBuffer buffer = getOrAllocateBufferForPage(maxPage).duplicate();
        while (buffer.remaining() > RecordEntry.FOOTER_LENGTH + RecordEntry.HEADER_LENGTH) {
            buffer.get(currentOffset, maybeHeader);
            int expectedSize = intFrom2Bytes(maybeHeader, 2);
            buffer.get(currentOffset + RecordEntry.HEADER_LENGTH + expectedSize, maybeFooter);
            if (!PlainRecordEntry.isValidHeader(maybeHeader) && !PlainRecordEntry.isValidFooter(maybeFooter) && expectedSize == 0) {
                // we've found
                updateFreeOffset(pageoffset + currentOffset);
                return;
            } else {
                currentOffset += expectedSize + RecordEntry.HEADER_LENGTH + RecordEntry.FOOTER_LENGTH;
            }
        }
        int page = maxPage + 1;
        getOrAllocateBufferForPage(page); // force initialization of next page
        updateFreeOffset((long) page * pageSize);
    }

    private Boolean initializeNewRecordEntry(int contentSize, ByteBuffer buffer) {
        if (!isZero(buffer)) {
            return false;
        }
        buffer.clear();
        buffer.put(PlainRecordEntry.HEADER);
        putInt2Bytes(buffer, contentSize);
        buffer.position(PlainRecordEntry.HEADER_LENGTH + contentSize);
        buffer.put(PlainRecordEntry.FOOTER);
        return true;
    }

    private boolean isZero(ByteBuffer buffer) {
        while (buffer.remaining() >= 8) {
            long aLong = buffer.getLong();
            if (aLong != 0) {
                return false;
            }
        }
        while (buffer.remaining() > 0) {
            byte b = buffer.get();
            if (b != 0) {
                return false;
            }
        }
        return true;
    }


    public <T> T executeLocked(long offset, int size, Function<ByteBuffer, T> function, boolean readLock) {
        int offsetInPage = (int) (offset % pageSize);
        int page = (int) (offset / pageSize);
        ByteBuffer buffer = getOrAllocateBufferForPage(page);
        ByteBuffer slice = buffer.slice(offsetInPage, size);
        RegionLock regionLock = getOrAllocateRegionLock(offset, size, offsetInPage, page);
        return doWithLock(function, readLock, slice, regionLock);
    }

    /*
     * TODO check all overlapping region locks instead of exact match
     */
    private RegionLock getOrAllocateRegionLock(long offset, int size, int offsetInPage, int page) {
        RegionLock regionLock = allocateNewRegionLock(page, offsetInPage, size); // this should prevent the inner soft reference from being collected
        SoftReference<RegionLock> reference = allocatedLocks.computeIfAbsent(offset, ignored -> new SoftReference<>(regionLock));
        RegionLock cached = reference.get();
        if (cached == null) {
            allocatedLocks.remove(offset, reference);
            return getOrAllocateRegionLock(offset, size, offsetInPage, page);
        }
        return cached;
    }

    private <T> T doWithLock(Function<ByteBuffer, T> function, boolean readLock, ByteBuffer slice, RegionLock lock) {
        if (readLock) {
            return lock.doWithSharedLock(() -> function.apply(slice));
        }
        return lock.doWithExclusiveLock(() -> function.apply(slice));
    }

    protected abstract RegionLock allocateNewRegionLock(int page, int offsetInPage, int size);

    protected abstract ByteBuffer getOrAllocateBufferForPage(int page);

    protected abstract Collection<Integer> knownPages();

    protected void updateFreeOffset(long newFreeOffset) {
        long l = freeOffset.get();
        while (l < newFreeOffset) {
            if (freeOffset.compareAndSet(l, newFreeOffset)) {
                return;
            }
            l = freeOffset.get();
        }
    }

    protected int pageNumberForAbsoluteOffset(long offset) {
        return (int) (offset / getPageSize());
    }

    protected long getLastUnusedOffset() {
        return freeOffset.get();
    }

    public int offsetInPage(long offset, int page) {
        return (int) (offset - page * pageSize);
    }

    @Override
    public void close() throws Exception {
    }
}
