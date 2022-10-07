package concurrencytest.runtime.tree;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.LockMonitorAcquisition;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.ThreadState;
import concurrencytest.runtime.tree.ByteBufferManager.RecordEntry;
import concurrencytest.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Format is
 * <p>
 * - parent offset (or 0 in case of uninitialized nodes) - 6 bytes
 * - length (4 bytes unsigned int)
 * - flags (1 byte)
 *      - all visited
 * - number of actors N (unsigned varint, up to 5 bytes)
 * - N entries of:
 *      - actorname (varstring), offset (6 bytes), threadInfo (var bytes)
 * <p>
 * if offset is 0, it is an unitialized node that will be initialized later
 * <p>
 * TODO make NodeLink have the size of the child node so it can be fetched in one go (and not require two)
 */
public class ByteBufferBackedTreeNode implements TreeNode {

    public static final int ALL_VISITED_FLAG = 1;

    public static final int FLAGS_OFFSET = 10;
    public static final int MAX_TREE_NODE_SIZE = FLAGS_OFFSET * 1024;

    public static final short END_OF_RECORD_MARKER = (short) 0xfeea;
    private static final byte FULLY_EXPLORED = 1;

    private final ByteBufferManager byteBufferManager;
    private final RecordEntry recordEntry;

    public ByteBufferBackedTreeNode(ByteBufferManager byteBufferManager, long offset, int size) {
        this.byteBufferManager = byteBufferManager;
        this.recordEntry = byteBufferManager.getExisting(offset, size);
    }

    public ByteBufferBackedTreeNode(ByteBufferManager byteBufferManager, RecordEntry entry) {
        this.byteBufferManager = byteBufferManager;
        this.recordEntry = entry;
    }

    private long parentOffset() {
        return recordEntry.readFromRecord(ByteBufferUtil::readLong6Bytes);
    }

    public static ByteBufferBackedTreeNode rootNode(long parentOffset, Collection<? extends String> actorNames, CheckpointRegister register, ByteBufferManager byteBufferManager) {
        ByteBuffer tempBuffer = initializeStates(parentOffset, byteBufferManager, actorNames.stream().map(actor -> new ActorInformation(actor, register.taskStartingCheckpoint().checkpointId())).toList());
        try {
            int size = tempBuffer.remaining();
            RecordEntry recordEntry = byteBufferManager.allocateNewSlice(size);
            recordEntry.overwriteRecord(tempBuffer);
            return new ByteBufferBackedTreeNode(byteBufferManager, recordEntry);
        } finally {
            byteBufferManager.returnBuffer(tempBuffer);
        }
    }

    public static ByteBuffer initializeTemporaryNodeBuffer(long parentOffset, Collection<? extends ThreadState> threadStates, ByteBufferManager byteBufferManager) {
        return initializeStates(parentOffset, byteBufferManager, toActorInformation(threadStates));
    }

    private static ByteBuffer initializeStates(long parentOffset, ByteBufferManager byteBufferManager, Collection<ActorInformation> information) {
        ByteBuffer tempBuffer = byteBufferManager.allocateTemporaryBuffer(MAX_TREE_NODE_SIZE);
        ByteBufferUtil.writeLong6Bytes(tempBuffer, parentOffset);
        tempBuffer.putInt(0); // temporary
        tempBuffer.put((byte) 0); // flags
        int count = 11;
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

    public record NodeLink(long currentOffset, long childOffset, ActorInformation information) {

        public static NodeLink readFromBuffer(ByteBuffer buffer) {
            long offset = ByteBufferUtil.readLong6Bytes(buffer);
            return new NodeLink(0, offset, ActorInformation.readFromBuffer(buffer));
        }

        public boolean canAdvance() {
            return !information.isBlocked();
        }

        public boolean isFullyExplored(ByteBufferManager byteBufferManager) {
            return (childOffset != 0 && !allocate(childOffset, byteBufferManager).isFullyExplored());
        }

    }

    public record RecordHeader(long parentOffset, int size, int flags) {

        public static final int SIZE = 11;

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
        return threadStates.stream().map(ts -> new ActorInformation(ts.actorName(), ts.checkpoint(), ts.loopCount(), toMonitorInformations(ts.ownedMonitors(), monitorOwners), toMonitorInformations(ts.ownedLocks(), lockOwners),
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
        if (this.recordEntry.offset() == parent) {
            return this;
        }
        return allocate(parent);
    }

    private TreeNode allocate(long offset) {
        return allocate(offset, byteBufferManager);
    }

    private static TreeNode allocate(long offset, ByteBufferManager byteBufferManager) {
        RecordEntry entry = byteBufferManager.getExisting(offset, RecordHeader.SIZE);
        RecordHeader header = entry.readFromRecord(RecordHeader::read);
        return new ByteBufferBackedTreeNode(byteBufferManager, offset, header.size);
    }

    protected synchronized List<NodeLink> childNodeLinks() {
        return recordEntry.readFromRecord(this::nodeLinksFromBuffer);
    }

    private List<NodeLink> nodeLinksFromBuffer(ByteBuffer bb) {
        bb.position(RecordHeader.SIZE);
        return ByteBufferUtil.readList(bb, NodeLink::readFromBuffer);
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
    public Stream<String> unexploredPaths() {
        return childNodeLinks().stream().filter(NodeLink::canAdvance).filter(n -> !n.isFullyExplored(byteBufferManager)).map(nl -> nl.information.actorName());
    }

    @Override
    public Optional<Supplier<TreeNode>> childNode(String nodeName) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public TreeNode advance(ThreadState selectedToProceed, RuntimeState next) {
        return this.recordEntry.writeToRecord(myBuffer -> {
            myBuffer.position(RecordHeader.SIZE);
            int entries = ByteBufferUtil.readVarInt(myBuffer);
            for (int i = 0; i < entries; i++) {
                myBuffer.mark();
                var node = NodeLink.readFromBuffer(myBuffer);
                if (node.information.actorName().equals(selectedToProceed.actorName())) {
                    if (node.childOffset() == 0) {
                        ByteBuffer byteBuffer = initializeTemporaryNodeBuffer(this.recordEntry.offset(), next.actorNamesToThreadStates().values(), this.byteBufferManager);
                        RecordEntry recordEntry = byteBufferManager.allocateNewSlice(byteBuffer.remaining());
                        recordEntry.overwriteRecord(byteBuffer);
                        myBuffer.reset();
                        ByteBufferUtil.writeLong6Bytes(myBuffer, recordEntry.offset());
                        return new ByteBufferBackedTreeNode(byteBufferManager, recordEntry);
                    } else {
                        return allocate(node.childOffset());
                    }
                }
            }
            throw new IllegalArgumentException("child node with name %s not found".formatted(selectedToProceed.actorName()));
        });
    }

    @Override
    public boolean isFullyExplored() {
        return (recordEntry.readFromRecord(bb -> bb.get(FLAGS_OFFSET)) & ALL_VISITED_FLAG) != 0;
    }

    @Override
    public void markFullyExplored() {
        this.recordEntry.writeToRecordNoReturn(bb -> {
            byte b = bb.get(FLAGS_OFFSET);
            bb.put(FLAGS_OFFSET, (byte) (b | FULLY_EXPLORED));

        });
    }
}
