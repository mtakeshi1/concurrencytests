package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface TreeNode {

    Supplier<TreeNode> parentNode();

    Map<String, ThreadState> threads();

    Map<String, Supplier<TreeNode>> childNodes();

    default Stream<String> unexploredPaths() {
        Map<String, Supplier<TreeNode>> nodes = this.childNodes();
        return threads().keySet().stream().filter(actor -> !nodes.containsKey(actor) || !nodes.get(actor).get().isFullyExplored());
    }

    default boolean hasUnexploredChildren() {
        return !isFullyExplored() && unexploredPaths().findAny().isPresent();
    }

    TreeNode advance(ThreadState selectedToProceed, RuntimeState next);

    boolean isFullyExplored();

    void markFullyExplored();

}
