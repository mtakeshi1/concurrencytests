package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;

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

    /**
     * If the returned Optional is not empty, it will generate the TreeNode corresponding to selecting the given actorName to run.
     * If it's empty(), it corresponds to an unexplored node.
     *
     * @param nodeName String
     */
    Optional<Supplier<TreeNode>> childNode(String nodeName);

    default boolean isInitialized(String nodeName) {
        return childNode(nodeName).isPresent();
    }

    Stream<String> unexploredPaths();


    default long maxKnownDepth() {
        return childNodes().values().stream().filter(Optional::isPresent).map(Optional::get).map(Supplier::get).mapToLong(tn -> 1 + tn.maxKnownDepth()).max().orElse(0);
    }

    default boolean hasUnexploredChildren() {
        return !isFullyExplored() && unexploredPaths().findAny().isPresent();
    }

    TreeNode advance(ThreadState selectedToProceed, RuntimeState next);

    boolean isFullyExplored();

    void markFullyExplored();

    void checkAllChildrenExplored();

    default boolean allFinished() {
        return threads().values().stream().allMatch(ActorInformation::finished);
    }
}
