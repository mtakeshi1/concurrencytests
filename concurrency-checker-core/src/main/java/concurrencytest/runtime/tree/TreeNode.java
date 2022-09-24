package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface TreeNode {
    Map<String, ThreadState> threads();

    Map<String, Supplier<TreeNode>> materializedChildNodes();

    default Stream<String> unexploredPaths() {
        Map<String, Supplier<TreeNode>> nodes = this.materializedChildNodes();
        return threads().keySet().stream().filter(actor -> !nodes.containsKey(actor) || !nodes.get(actor).get().isFullyExplored());
    }

    default boolean hasUnexploredChildren() {
        return !isFullyExplored() && unexploredPaths().count() != 0;
    }

    TreeNode advanced(ThreadState selectedToProceed, RuntimeState next);

    boolean isFullyExplored();

    void markFullyExplored();

}
