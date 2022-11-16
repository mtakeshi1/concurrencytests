package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.util.Utils;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface TreeNode {

    TreeNode EMPTY_TREE_NODE = new TreeNode() {
        @Override
        public TreeNode parentNode() {
            return this;
        }

        @Override
        public Map<String, ActorInformation> threads() {
            return Map.of();
        }

        @Override
        public Map<String, Optional<Supplier<TreeNode>>> childNodes() {
            return Map.of();
        }

        @Override
        public Optional<Supplier<TreeNode>> childNode(String nodeName) {
            return Optional.empty();
        }

        @Override
        public Stream<String> unexploredPaths() {
            return Stream.empty();
        }

        @Override
        public TreeNode advance(ThreadState selectedToProceed, RuntimeState next) {
            throw new IllegalArgumentException("cannot advance empty tree node");
        }

        @Override
        public boolean isFullyExplored() {
            return true;
        }

        @Override
        public void markFullyExplored() {
        }

        @Override
        public void checkAllChildrenExplored() {
        }
    };


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

    default void markLinkAsStartingPoint(String actor) {
        Utils.todo();
    }

}
