package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;

import java.util.Optional;
import java.util.function.Supplier;

public interface Tree {

    Optional<TreeNode> getRootNode();

    TreeNode getOrInitializeRootNode(Supplier<RuntimeState> runtimeState);
}
