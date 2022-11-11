package concurrencytest.runtime.tree.offheap;

import concurrencytest.runner.ActorSchedulerSetupTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static concurrencytest.util.FileUtils.deltree;

public class OffHeapByteBufferManagerTest {

    private ByteBufferManager byteBufferManager;
    private Path folder;

    @Before
    public void setup() throws IOException {
        this.folder = Files.createTempDirectory("bla");
        byteBufferManager = new OffHeapByteBufferManager(folder.toFile(), 100 * 1024);
    }

    @Test
    public void testSimpleAllocate() throws IOException{
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
    }

    @After
    public void tearDown() throws Exception {
        if (byteBufferManager != null) {
            byteBufferManager.close();
        }
        if (folder != null) {
            deltree(folder.toFile());
        }
    }


}