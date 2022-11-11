package concurrencytest.runtime.tree;

import concurrencytest.runtime.tree.offheap.ByteBufferManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ByteBufferManagerTest {


    private ByteBufferManager byteBufferManager;

    @Before
    public void setup() {
        byteBufferManager = new HeapByteBufferManager();
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

}
