package concurrencytest.runtime.tree.offheap;

import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReadWriteLock;

public record PageLock(int pageIndex, ReadWriteLock lock, FileChannel channel) {
}
