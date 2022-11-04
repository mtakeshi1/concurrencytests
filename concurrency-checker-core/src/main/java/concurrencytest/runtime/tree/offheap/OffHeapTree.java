package concurrencytest.runtime.tree.offheap;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;

import java.util.Collection;
import java.util.Optional;

public class OffHeapTree implements Tree {

    private ByteBufferManager byteBufferManager;

    private volatile TreeNode root;

    public OffHeapTree() {
    }

    @Override
    public Optional<TreeNode> getRootNode() {
        return Optional.ofNullable(root);
    }

    @Override
    public synchronized TreeNode getOrInitializeRootNode(Collection<? extends String> actorNames, CheckpointRegister register) {
        if (root != null) {
            return root;
        }
        this.root = ByteBufferBackedTreeNode.rootNode(0, actorNames, register, byteBufferManager);
        return root;
    }

}
