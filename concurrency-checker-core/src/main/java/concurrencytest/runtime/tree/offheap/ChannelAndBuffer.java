package concurrencytest.runtime.tree.offheap;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public record ChannelAndBuffer(int pageIndex, File file, FileChannel channel, ByteBuffer buffer) {
}
