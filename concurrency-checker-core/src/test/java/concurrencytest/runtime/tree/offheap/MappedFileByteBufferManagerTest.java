package concurrencytest.runtime.tree.offheap;

import concurrencytest.runtime.tree.ByteBufferManagerTest;
import org.junit.After;

import java.nio.file.Files;
import java.nio.file.Path;

import static concurrencytest.util.FileUtils.deltree;

public class MappedFileByteBufferManagerTest extends ByteBufferManagerTest {

    private Path folder;

    @Override
    protected ByteBufferManager instantiateByteBuffer(int bufferSize) throws Exception {
        this.folder = Files.createTempDirectory("bla");
        return new MappedFileByteBufferManager(folder.toFile(), bufferSize);
    }

    @After
    public void tearDown() throws Exception {
        if (folder != null) {
            deltree(folder.toFile());
        }
    }

}