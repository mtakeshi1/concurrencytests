package concurrencytest.runtime.tree;

import concurrencytest.runtime.tree.offheap.ByteBufferManager;
import concurrencytest.runtime.tree.offheap.ByteBufferManager.RecordEntry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

public abstract class ByteBufferManagerTest {

    public static final int PAGE_SIZE = 4 * 1024;

    private ByteBufferManager byteBufferManager;

    protected abstract ByteBufferManager instantiateByteBuffer(int pageSize) throws Exception;

    @Before
    public final void setup() throws Exception {
        byteBufferManager = instantiateByteBuffer(PAGE_SIZE);
    }

    @After
    public final void dispose() throws Exception {
        if (byteBufferManager != null) {
            byteBufferManager.close();
        }
    }

    @Test
    public void basicAllocationTests() throws Exception {
        ByteBufferManager.RecordEntry recordEntry = byteBufferManager.allocateNewSlice(1024);
        Assert.assertNotNull(recordEntry);
        Assert.assertEquals(1024, recordEntry.contentSize());
        recordEntry.readFromRecordNoReturn(b -> Assert.assertEquals(1024, b.remaining()));
        recordEntry.readFromRecordNoReturn(b -> {
            while (b.remaining() > 0) {
                int offset = b.position();
                Assert.assertEquals("error at position: %d".formatted(offset), 0L, b.getLong());
            }
        });
        recordEntry.writeToRecordNoReturn(b -> Assert.assertEquals(1024, b.remaining()));
    }

    @Test
    public void writeReadTest() throws Exception {
        ByteBufferManager.RecordEntry recordEntry = byteBufferManager.allocateNewSlice(1024);
        recordEntry.writeToRecordNoReturn(b -> {
            byte c = 0;
            while (b.remaining() > 0) {
                b.put(c++);
            }
        });
        recordEntry.readFromRecordNoReturn(b -> {
            byte c = 0;
            while (b.remaining() > 0) {
                Assert.assertEquals(c++, b.get());
            }
        });
    }

    @Test
    public void multipleRandomRecors() throws IOException {
        for (int seed = 0; seed < 1000; seed++) {
            int sampleSize = 32;
            int[] possibleSizes = {64, 128, 256, 512};
            byte[] initialValues = new byte[sampleSize];
            byte[] increments = new byte[sampleSize];
            int[] recordSizes = new int[sampleSize];
            Random r = new Random(seed);
            RecordEntry[] records = new RecordEntry[sampleSize];
            for (int i = 0; i < sampleSize; i++) {
                initialValues[i] = (byte) (1 + r.nextInt(255));
                increments[i] = (byte) (1 + r.nextInt(255));
                recordSizes[i] = possibleSizes[r.nextInt(possibleSizes.length)];
                records[i] = byteBufferManager.allocateNewSlice(recordSizes[i]);
                int current = i;
                records[i].writeToRecordNoReturn(buffer -> {
                    byte initial = initialValues[current];
                    byte increment = increments[current];
                    while (buffer.hasRemaining()) {
                        buffer.put(initial);
                        initial += increment;
                    }
                });
            }
            for (int i = 0; i < sampleSize; i++) {
                int current = i;
                records[i].readFromRecordNoReturn(buffer -> {
                    byte initial = initialValues[current];
                    byte increment = increments[current];
                    while (buffer.hasRemaining()) {
                        byte found = buffer.get();
                        Assert.assertEquals(initial, found);
                        initial += increment;
                    }
                });
            }
        }
    }

    @Test
    public void invalidContentSizes() {
        failRecordSize(0);
        failRecordSize(-1);
        failRecordSize(RecordEntry.MAX_RECORD_LENGTH);
        failRecordSize(RecordEntry.MAX_RECORD_LENGTH - RecordEntry.FOOTER_LENGTH);
        failRecordSize(RecordEntry.MAX_RECORD_LENGTH - RecordEntry.HEADER_LENGTH);
        failRecordSize(PAGE_SIZE + 1);
    }

    @Test
    public void fringeRecordSizes() {
        byteBufferManager.allocateNewSlice(1);
        byteBufferManager.allocateNewSlice(Math.min(RecordEntry.MAX_RECORD_LENGTH, byteBufferManager.getPageSize() - RecordEntry.HEADER_LENGTH - RecordEntry.FOOTER_LENGTH));
    }

    private void failRecordSize(int size) {
        try {
            byteBufferManager.allocateNewSlice(size);
            Assert.fail("should have failed for size " + size);
        } catch (IllegalArgumentException e) {
            //ignored
        }
    }

}
