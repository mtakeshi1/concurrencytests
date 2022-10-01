package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.ThreadState;

import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface TreeNode {


    default boolean isRootNode() {
        return this == parentNode();
    }

    TreeNode parentNode();

    Map<String, ActorInformation> threads();

    Map<String, Optional<Supplier<TreeNode>>> childNodes();

    default Stream<String> unexploredPaths() {
//        Map<String, Supplier<TreeNode>> nodes = this.childNodes();
//        return threads().keySet().stream().filter(actor -> !nodes.containsKey(actor) || !nodes.get(actor).get().isFullyExplored());
        throw new RuntimeException("not yet implemented");
    }

    default boolean hasUnexploredChildren() {
        return !isFullyExplored() && unexploredPaths().findAny().isPresent();
    }

    TreeNode advance(ThreadState selectedToProceed, RuntimeState next);

    boolean isFullyExplored();

    void markFullyExplored();

    default boolean allFinished() {
        return threads().values().stream().allMatch(ActorInformation::finished);
    }
}
