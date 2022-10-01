package concurrencytest.runtime.tree;

import concurrencytest.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

public record ActorInformation(String actorName, List<LockOrMonitorInformation> monitorsOwned, List<LockOrMonitorInformation> locksLocked,
                               Optional<LockOrMonitorInformation> waitingForMonitor, Optional<LockOrMonitorInformation> waitingForLock,
                               boolean finished) {

    public static final int WAITING_FOR_MONITOR_FLAG = 1;
    public static final int WAITING_FOR_LOCK_FLAG = 2;
    public static final int FINISHED_FLAG = 2;

    public int writeToByteBuffer(ByteBuffer buffer) {
        int c = ByteBufferUtil.writeString(buffer, actorName);
        c += ByteBufferUtil.writeCollection(buffer, monitorsOwned, (a, b) -> b.writeToByteBuffer(a));
        c += ByteBufferUtil.writeCollection(buffer, locksLocked, (a, b) -> b.writeToByteBuffer(a));
        byte flags = (byte) ((waitingForMonitor.isPresent() ? WAITING_FOR_MONITOR_FLAG : 0) + (waitingForLock.isPresent() ? WAITING_FOR_LOCK_FLAG : 0) + (finished ? FINISHED_FLAG : 0));
        buffer.put(flags);
        c++;
        c += waitingForMonitor().map(lmi -> lmi.writeToByteBuffer(buffer)).orElse(0);
        c += waitingForLock().map(lmi -> lmi.writeToByteBuffer(buffer)).orElse(0);
        return c;
    }

    public static ActorInformation readFromBuffer(ByteBuffer byteBuffer) {
        byteBuffer.mark();
        String actorName = ByteBufferUtil.readString(byteBuffer);
        List<LockOrMonitorInformation> monitors = ByteBufferUtil.readList(byteBuffer, LockOrMonitorInformation::readFromBuffer);
        List<LockOrMonitorInformation> locks = ByteBufferUtil.readList(byteBuffer, LockOrMonitorInformation::readFromBuffer);
        int flags = byteBuffer.get() & 0xff;
        Optional<LockOrMonitorInformation> monitorWaiting = Optional.empty();
        if ((flags & WAITING_FOR_MONITOR_FLAG) != 0) {
            monitorWaiting = Optional.of(LockOrMonitorInformation.readFromBuffer(byteBuffer));
        }
        Optional<LockOrMonitorInformation> lockWaiting = Optional.empty();
        if ((flags & WAITING_FOR_LOCK_FLAG) != 0) {
            lockWaiting = Optional.of(LockOrMonitorInformation.readFromBuffer(byteBuffer));
        }
        byteBuffer.reset();
        return new ActorInformation(actorName, monitors, locks, monitorWaiting, lockWaiting, (flags & FINISHED_FLAG) != 0);
    }

}
