package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.ThreadState;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Format should be something like this:
 *
 * - flags
 *  - all visited
 * - number of actors
 *  - actorname,threadInfo,offset (6 bytes)
 *
 * if offset is 0, it is an unitialized node that will be initialized later
 *
 */
public class ByteBufferBackedTreeNode implements TreeNode {

    public static final long OFFSET_MASK = 0xffffffffffffL;

    private final ByteBufferManager byteBufferManager;

    public ByteBufferBackedTreeNode(ByteBufferManager byteBufferManager) {
        this.byteBufferManager = byteBufferManager;
    }


    @Override
    public Supplier<TreeNode> parentNode() {
        return null;
    }

    @Override
    public Map<String, ActorInformation> threads() {
        return null;
    }

    @Override
    public Map<String, Supplier<TreeNode>> childNodes() {
        return null;
    }

    @Override
    public TreeNode advance(ThreadState selectedToProceed, RuntimeState next) {
        return null;
    }

    @Override
    public boolean isFullyExplored() {
        return false;
    }

    @Override
    public void markFullyExplored() {

    }
}
