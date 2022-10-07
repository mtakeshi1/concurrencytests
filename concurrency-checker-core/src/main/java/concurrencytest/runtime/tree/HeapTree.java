package concurrencytest.runtime.tree;

import concurrencytest.checkpoint.CheckpointRegister;

import java.util.Collection;
import java.util.Optional;

public class HeapTree implements Tree {

    private volatile TreeNode root;


    @Override
    public Optional<TreeNode> getRootNode() {
        return Optional.ofNullable(root);
    }

    @Override
    public synchronized TreeNode getOrInitializeRootNode(Collection<? extends String> actorNames, CheckpointRegister register) {
        if (root == null) {
            root = PlainTreeNode.rootNode(actorNames, register);
        }
        return root;
    }
}
