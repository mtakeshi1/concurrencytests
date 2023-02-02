package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.util.Utils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A tree node represents a state that reached at some point. The root tree node represents the begginning of all possible reachable states.
 * <p>
 * Tree nodes are linked to their children by 'actor name' and each link represents what happens when you select that actor to resume from that state.
 * <p>
 * TreeNodes marked as starting point represent TreeNodes that have a scheduler started on them and will eventually explore all of its children.
 * In other words, they shouldn't be selected to be scheduled except by the scheduler that has marked the TreeNode as a starting point. This is to support
 * 'forking' of new schedulers to mantain a certain level of parallelism
 * <p>
 * Within each tree node, we have the following information
 * - state of each actor
 * - if this tree node is fully explored
 * - unexplored paths - meaning, actors that can still be resumed to find new states
 * - if this node is marked as a Starting point.
 * <p>
 * A treeNode that is fully explored is allowed to 'clean up', meaning no longer hold the actor states and links and therefore may not be able to traverse further.
 * <p>
 * TODO: {@link TreeNode#advance(ThreadState, RuntimeState)} should be enough to {@link TreeNode#markFullyExplored()} and should not depend on external actors to do so
 */
public interface TreeNode {

    /**
     * Constant to be used when a tree node is fully explored.
     */
    TreeNode EMPTY_TREE_NODE = new TreeNode() {

        @Override
        public boolean isRootNode() {
            return false;
        }

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


    /**
     * @return true if this is a root node
     */
    default boolean isRootNode() {
        return this == parentNode();
    }

    /**
     * @return TreeNode that represents the parent, or this
     */
    TreeNode parentNode();

    /**
     * @return A map with the actor states, in a persistable representation (a representation that does not depend on external information)
     */
    Map<String, ActorInformation> threads();

    /**
     * Return a map with all known child nodes, as per {@link TreeNode#childNode(String)}
     */
    Map<String, Optional<Supplier<TreeNode>>> childNodes();

    /**
     * Returns a child node if it has been visited. If its empty, the node has not been visited.
     *
     * @param nodeName String
     */
    Optional<Supplier<TreeNode>> childNode(String nodeName);

    /**
     * Checks if a particular link was initialized
     *
     * @param nodeName the actor name
     */
    default boolean isInitialized(String nodeName) {
        return childNode(nodeName).isPresent();
    }

    /**
     * @return a Stream of actors whose paths are not completely explored
     */
    Stream<String> unexploredPaths();

    /**
     * This method tries to discover the max known depth starting from this node. It should give an indication of the number of scheduling necessary until all of the threads are finished.
     * <p>
     * This method is a best-effort and its results should only be used for statistical pourposes
     */
    default long maxKnownDepth() {
        return childNodes().values().stream().filter(Optional::isPresent).map(Optional::get).map(Supplier::get).mapToLong(tn -> 1 + tn.maxKnownDepth()).max().orElse(0);
    }

    /**
     * @return true if this node has unexplored children
     */
    default boolean hasUnexploredChildren() {
        return !isFullyExplored() && unexploredPaths().findAny().isPresent();
    }

    /**
     * Initializes a child tree node that represents resuming the given thread, resulting in the given state.
     *
     * @param selectedToProceed the actor that resumed
     * @param next              the system state resulting of resuming the actor
     * @return the freshly created TreeNode
     */
    TreeNode advance(ThreadState selectedToProceed, RuntimeState next);

    /**
     * If true, this tree node is already exausted and should no longer be selected as part of a scheduling
     */
    boolean isFullyExplored();

    /**
     * Marks this node as fully explored. This method can recursively mark parent nodes that are fully explored and may also
     * clear state (to free some memory)
     */
    void markFullyExplored();

    /**
     * Tries to check if all of this TreeNode's children are marked as explored. There's a race condition between different scheduling competing to see
     * who marks a TreeNode as completed, so this method helps competing threads to finish the marking
     */
    void checkAllChildrenExplored();

    /**
     * @return true for terminal state tree nodes.
     */
    default boolean allFinished() {
        return threads().values().stream().allMatch(ActorInformation::finished);
    }

    default void markLinkAsStartingPoint(String actor) {
        Utils.todo();
    }

    default boolean isLinkStartingPoint(String link) {
        return Utils.todo();
    }

    default ArrayList<String> pathFromRoot() {
        return new ArrayList<>();
    }


}
