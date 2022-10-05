package concurrencytest.runtime.tree;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RuntimeState;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public interface Tree {

    Optional<TreeNode> getRootNode();

//    TreeNode getOrInitializeRootNode(Supplier<RuntimeState> runtimeState);

    TreeNode getOrInitializeRootNode(Collection<? extends String> actorNames, CheckpointRegister register);


}
