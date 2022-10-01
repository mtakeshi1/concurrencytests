package concurrencytest.runtime.tree;

import concurrencytest.runtime.LockMonitorAcquisition;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.ThreadState;
import concurrencytest.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Format should be something like this:
 * <p>
 * - parent offset (or 0) - 6 bytes
 * - length
 * - flags
 * - all visited
 * - number of actors
 * - actorname,threadInfo,offset (6 bytes)
 * <p>
 * if offset is 0, it is an unitialized node that will be initialized later
 */
public class ByteBufferBackedTreeNode implements TreeNode {

    public static final long OFFSET_MASK = 0xffffffffffffL;

    public static final int ALL_VISITED_FLAG = 1;

    public static final int MAX_TREE_NODE_SIZE = 10 * 1024;

    public static final int HEADER_SIZE = 11;

    public static final short END_OF_RECORD_MARKER = (short) 0xfeea;


    private final ByteBufferManager byteBufferManager;
    private final ByteBuffer buffer;
    private final long offset;

    public ByteBufferBackedTreeNode(ByteBufferManager byteBufferManager, long offset, int size) {
        this.byteBufferManager = byteBufferManager;
        this.offset = offset;
        this.buffer = byteBufferManager.getExisting(offset, size);
    }

    private long parentOffset() {
        return ByteBufferUtil.readLong6Bytes(buffer, 0);
    }

    public static ByteBufferBackedTreeNode initializeNode(long parentOffset, Collection<? extends ThreadState> threadStates, ByteBufferManager byteBufferManager) {
        ByteBuffer tempBuffer = initializeTemporary(parentOffset, threadStates, byteBufferManager);
        try {
            int size = tempBuffer.remaining();
            long offset = byteBufferManager.allocateNewSlice(size);
            byteBufferManager.executeLocked(offset, size, bb -> bb.put(tempBuffer));
            return new ByteBufferBackedTreeNode(byteBufferManager, offset, size);
        } finally {
            byteBufferManager.returnBuffer(tempBuffer);
        }
    }

    public static ByteBuffer initializeTemporary(long parentOffset, Collection<? extends ThreadState> threadStates, ByteBufferManager byteBufferManager) {
        ByteBuffer tempBuffer = byteBufferManager.allocateTemporaryBuffer(MAX_TREE_NODE_SIZE);
        ByteBufferUtil.writeLong6Bytes(tempBuffer, parentOffset);
        tempBuffer.putInt(0); // temporary
        tempBuffer.put((byte) 0); // flags
        int count = 11;
        Collection<ActorInformation> information = toActorInformation(threadStates);
        count += ByteBufferUtil.writeCollection(tempBuffer, information, (b, ai) -> {
            ByteBufferUtil.writeLong6Bytes(b, 0);
            return 6 + ai.writeToByteBuffer(tempBuffer);
        });
        tempBuffer.putShort(END_OF_RECORD_MARKER);
        count += 2;
        tempBuffer.putInt(count, 6);
        tempBuffer.flip();
        return tempBuffer;
    }

    public static record NodeLink(long currentOffset, long childOffset, ActorInformation information) {

        public static NodeLink readFromBuffer(ByteBuffer buffer) {
            long offset = ByteBufferUtil.readLong6Bytes(buffer);
            return new NodeLink(0, offset, ActorInformation.readFromBuffer(buffer));
        }
    }

    public static record RecordHeader(long parentOffset, int size, int flags) {
        public static RecordHeader read(ByteBuffer buffer) {
            long parent = ByteBufferUtil.readLong6Bytes(buffer);
            int size = buffer.getInt();
            int flags = buffer.get() & 0xff;
            return new RecordHeader(parent, size, flags);
        }
    }


    public static Collection<ActorInformation> toActorInformation(Collection<? extends ThreadState> threadStates) {
        Map<Integer, String> monitorOwners = new HashMap<>();
        Map<Integer, String> lockOwners = new HashMap<>();
        for (ThreadState state : threadStates) {
            for (LockMonitorAcquisition monitor : state.ownedMonitors()) {
                monitorOwners.put(monitor.lockOrMonitorId(), state.actorName());
            }
            for (LockMonitorAcquisition lock : state.ownedLocks()) {
                lockOwners.put(lock.lockOrMonitorId(), state.actorName());
            }
        }
        return threadStates.stream().map(ts -> new ActorInformation(ts.actorName(), toMonitorInformations(ts.ownedMonitors(), monitorOwners), toMonitorInformations(ts.ownedLocks(), lockOwners),
                ts.waitingForMonitor().map(lma -> toMonitorLockInformation(monitorOwners, lma)), ts.waitingForLock().map(lma1 -> toMonitorLockInformation(lockOwners, lma1)), ts.finished())
        ).toList();
    }

    private static LockOrMonitorInformation toMonitorLockInformation(Map<Integer, String> monitorOwners, LockMonitorAcquisition lma) {
        return new LockOrMonitorInformation(
                lma.aquisitionCheckpoint().details(), Optional.ofNullable(monitorOwners.get(lma.lockOrMonitorId())), lma.aquisitionCheckpoint().sourceFile(), lma.aquisitionCheckpoint().lineNumber()
        );
    }

    private static List<LockOrMonitorInformation> toMonitorInformations(List<LockMonitorAcquisition> monitors, Map<Integer, String> monitorOwners) {
        return monitors.stream().map(lma -> toMonitorLockInformation(monitorOwners, lma)).toList();
    }

    @Override
    public TreeNode parentNode() {
        long parent = parentOffset();
        if (this.offset == parent) {
            return this;
        }
        return allocate(parent);
    }

    private TreeNode allocate(long offset) {
        ByteBuffer buffer = byteBufferManager.getExisting(offset, HEADER_SIZE);
        RecordHeader header = RecordHeader.read(buffer);
        return new ByteBufferBackedTreeNode(byteBufferManager, offset, header.size);
    }

    protected synchronized List<NodeLink> childNodeLinks() {
        buffer.position(HEADER_SIZE);
        return ByteBufferUtil.readList(buffer, NodeLink::readFromBuffer);
    }

    @Override
    public Map<String, ActorInformation> threads() {
        return childNodeLinks().stream().collect(Collectors.toMap(nodeLink -> nodeLink.information().actorName(), NodeLink::information));
    }

    @Override
    public Map<String, Optional<Supplier<TreeNode>>> childNodes() {
        Map<String, Optional<Supplier<TreeNode>>> map = new HashMap<>();
        for (NodeLink link : childNodeLinks()) {
            if (link.childOffset == 0) {
                map.put(link.information.actorName(), Optional.empty());
            } else {
                map.put(link.information.actorName(), Optional.of(() -> allocate(link.childOffset)));
            }
        }
        return map;
    }

    @Override
    public TreeNode advance(ThreadState selectedToProceed, RuntimeState next) {
        for (NodeLink link : childNodeLinks()) {
            if (link.information.actorName().equals(selectedToProceed.actorName())) {
                if (link.childOffset == 0) {
                    ByteBuffer byteBuffer = initializeTemporary(this.offset, next.actorNamesToThreadStates().values(), this.byteBufferManager);
                    throw new RuntimeException("not yet implemented");
//                    byteBufferManager.executeLocked(this.offset, buffer.capacity(), );
                } else {
                    return allocate(link.childOffset);
                }
            }
        }
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public boolean isFullyExplored() {
        return (buffer.get(10) & ALL_VISITED_FLAG) != 0;
    }

    @Override
    public void markFullyExplored() {
        this.byteBufferManager.executeLocked(this.offset + 10, 1, bb -> bb.put((byte) 1));
    }
}
