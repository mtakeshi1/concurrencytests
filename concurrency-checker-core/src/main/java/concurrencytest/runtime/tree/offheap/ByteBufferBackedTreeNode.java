package concurrencytest.runtime.tree.offheap;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.runtime.tree.ActorInformation;
import concurrencytest.runtime.tree.BlockingCause;
import concurrencytest.runtime.tree.ResourceInformation;
import concurrencytest.runtime.tree.TreeNode;
import concurrencytest.runtime.tree.offheap.ByteBufferManager.RecordEntry;
import concurrencytest.util.ByteBufferUtil;
import concurrencytest.util.Utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Format is
 * <p>
 * - parent offset (or 0 in case of uninitialized nodes or the root node) - 6 bytes
 * - flags (1 byte)
 * - all visited
 * - number of actors N (unsigned varint, up to 5 bytes)
 * - N entries of:
 * - offset (6 bytes),
 * - entry size (2 bytes unsigned int)
 * - threadInfo (var bytes)
 * <p>
 * if offset is 0, it is an unitialized node that will be initialized later
 * <p>
 * TODO make NodeLink have the size of the child node so it can be fetched in one go (and not require two)
 */
public class ByteBufferBackedTreeNode implements TreeNode {

    public static final int ALL_VISITED_FLAG = 1;

    public static final int FLAGS_OFFSET = 10;
    public static final int MAX_TREE_NODE_SIZE = FLAGS_OFFSET * 1024;

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
        try {
            return recordEntry.readFromRecord(ByteBufferUtil::readLong6Bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ByteBufferBackedTreeNode rootNode(long parentOffset, Collection<? extends String> actorNames, CheckpointRegister register, ByteBufferManager byteBufferManager) throws IOException {
        ByteBuffer tempBuffer = initializeStates(parentOffset, actorNames.stream().map(actor -> new ActorInformation(actor, register.taskStartingCheckpoint().checkpointId())).toList());
        int size = tempBuffer.remaining();
        RecordEntry recordEntry = byteBufferManager.allocateNewSlice(size);
        recordEntry.overwriteRecord(tempBuffer);
        return new ByteBufferBackedTreeNode(byteBufferManager, recordEntry);
    }

    /**
     * Creates a temporary bytebuffer that will have information from the runtime state. The returned buffer is positioned to read its content (the serialized form of the RuntimeState)
     *
     * @param parentOffset the offset of the parent node
     * @param state        the state to store
     * @return ByteBuffer allocated on the heap
     */
    public static ByteBuffer initializeTemporaryNodeBuffer(long parentOffset, RuntimeState state) {
        return initializeStates(parentOffset, toActorInformation(state, state.actorNamesToThreadStates().values()));
    }

    /**
     * Creates a temporary buffer that will have the actors stored. The link to the actor nodes will be zero. The returned byte buffer is a heap bytebuffer, positioned to read its content
     *
     * @param parentOffset the parent offset
     * @param information  the actor informations
     * @return ByteBuffer allocated on the heap
     */
    private static ByteBuffer initializeStates(long parentOffset, Collection<ActorInformation> information) {
        ByteBuffer tempBuffer = ByteBuffer.allocate(MAX_TREE_NODE_SIZE);
        ByteBufferUtil.writeLong6Bytes(tempBuffer, parentOffset);
        tempBuffer.put((byte) 0); // flags
        ByteBufferUtil.writeCollection(tempBuffer, information, (b, ai) -> {
            ByteBufferUtil.writeLong6Bytes(b, 0);
            ByteBufferUtil.writeInt2Bytes(b, 2);
            return 8 + ai.writeToByteBuffer(tempBuffer);
        });
        tempBuffer.flip();
        return tempBuffer;
    }

    public static Collection<ActorInformation> toActorInformation(RuntimeState state, Collection<? extends ThreadState> threadStates) {
        Map<BlockingResource, List<String>> resourceOwners = new HashMap<>();
        for (ThreadState threadState : threadStates) {
            for (BlockingResource resource : threadState.ownedResources()) {
                resourceOwners.computeIfAbsent(resource, ignored -> new ArrayList<>()).add(threadState.actorName());
            }
        }
        List<ActorInformation> list = new ArrayList<>();
        for (ThreadState threadState : threadStates) {
            Optional<BlockingCause> blockCause = threadState.resourceDependency().map(cause -> new BlockingCause(cause.type(), cause.ownedBy(state).stream().map(ThreadState::actorName).filter(name -> !name.equals(threadState.actorName())).findAny()));
            list.add(new ActorInformation(threadState.actorName(), threadState.checkpoint(), threadState.loopCount(), threadState.ownedResources().stream().map(res -> toResourceInformation(threadState.actorName(), res, resourceOwners)).toList(), blockCause, threadState.finished()));
        }
        return list;
    }

    private static ResourceInformation toResourceInformation(String actor, BlockingResource blockingResource, Map<BlockingResource, List<String>> resourceOwners) {
        var orDefault = resourceOwners.getOrDefault(blockingResource, Collections.emptyList()).stream().filter(owner -> !actor.equals(owner)).findAny();
        return new ResourceInformation(blockingResource.lockType() + " ( " + blockingResource.resourceType().getName() + " ) ", orDefault, blockingResource.lockType(), blockingResource.sourceCode(), blockingResource.lineNumber());
    }

    @Override
    public TreeNode parentNode() {
        long parent = parentOffset();
        if (this.recordEntry.absoluteOffset() == parent) {
            return this;
        }
        return allocateExisting(parent);
    }

    private TreeNode allocateExisting(long offset) {

        return null;
    }

    @Override
    public Map<String, ActorInformation> threads() {
//        return childNodeLinks().stream().collect(Collectors.toMap(nodeLink -> nodeLink.information().actorName(), NodeLink::information));
        return Utils.todo();
    }

    private record NodeLink(long currentOffset, long childNodeOffset, ActorInformation information) {

    }

    @Override
    public Map<String, Optional<Supplier<TreeNode>>> childNodes() {
//        Map<String, Optional<Supplier<TreeNode>>> map = new HashMap<>();
//        for (NodeLink link : childNodeLinks()) {
//            if (link.childOffset == 0) {
//                map.put(link.information.actorName(), Optional.empty());
//            } else {
//                map.put(link.information.actorName(), Optional.of(() -> allocate(link.childOffset)));
//            }
//        }
//        return map;
        return Utils.todo();
    }

    private Collection<? extends NodeLink> childNodeLinks() {
        return Utils.todo();
    }

    @Override
    public Stream<String> unexploredPaths() {
//        return childNodeLinks().stream().filter(NodeLink::canAdvance).filter(n -> !n.isFullyExplored(byteBufferManager)).map(nl -> nl.information.actorName());
        return Utils.todo();
    }

    @Override
    public Optional<Supplier<TreeNode>> childNode(String nodeName) {
        return Utils.todo();
    }

    @Override
    public TreeNode advance(ThreadState selectedToProceed, RuntimeState next) {
//        return this.recordEntry.writeToRecord(myBuffer -> {
//            myBuffer.position(RecordHeader.SIZE);
//            int entries = ByteBufferUtil.readVarInt(myBuffer);
//            for (int i = 0; i < entries; i++) {
//                myBuffer.mark();
//                var node = NodeLink.readFromBuffer(myBuffer);
//                if (node.information.actorName().equals(selectedToProceed.actorName())) {
//                    if (node.childOffset() == 0) {
//                        ByteBuffer byteBuffer = initializeTemporaryNodeBuffer(this.recordEntry.offset(), next);
//                        RecordEntry recordEntry = byteBufferManager.allocateNewSlice(byteBuffer.remaining());
//                        recordEntry.overwriteRecord(byteBuffer);
//                        myBuffer.reset();
//                        ByteBufferUtil.writeLong6Bytes(myBuffer, recordEntry.offset());
//                        return new ByteBufferBackedTreeNode(byteBufferManager, recordEntry);
//                    } else {
//                        return allocate(node.childOffset());
//                    }
//                }
//            }
//            throw new IllegalArgumentException("child node with name %s not found".formatted(selectedToProceed.actorName()));
//        });
        return Utils.todo();
    }

    @Override
    public boolean isFullyExplored() {
        try {
            return (recordEntry.readFromRecord(bb -> bb.get(FLAGS_OFFSET)) & ALL_VISITED_FLAG) != 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void markFullyExplored() {
        try {
            this.recordEntry.writeToRecordNoReturn(bb -> {
                byte b = bb.get(FLAGS_OFFSET);
                bb.put(FLAGS_OFFSET, (byte) (b | FULLY_EXPLORED));

            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void checkAllChildrenExplored() {
        Utils.todo();
    }
}
