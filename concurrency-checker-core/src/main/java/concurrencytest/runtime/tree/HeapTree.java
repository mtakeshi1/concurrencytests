package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;

import java.util.Optional;
import java.util.function.Supplier;

public class HeapTree implements Tree {

    private ByteBufferManager byteBufferManager;

    private volatile TreeNode root;

    public HeapTree() {
    }

    @Override
    public Optional<TreeNode> getRootNode() {
        return Optional.ofNullable(root);
    }

    @Override
    public synchronized TreeNode getOrInitializeRootNode(Supplier<RuntimeState> runtimeState) {
        if (root != null) {
            return root;
        }
        this.root = ByteBufferBackedTreeNode.initializeNode(0, runtimeState.get().actorNamesToThreadStates().values(), byteBufferManager);
        return root;
    }
}
