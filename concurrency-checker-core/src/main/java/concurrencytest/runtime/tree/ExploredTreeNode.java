package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Should be use to prune tree branches that are fully explored.
 * @param parent
 */
public record ExploredTreeNode(TreeNode parent) implements TreeNode {
    public ExploredTreeNode() {
        this(null);
    }

    @Override
    public TreeNode parentNode() {
        return parent == null ? this : parent;
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
        throw new IllegalArgumentException("cannot proceed on fully explored tree node");
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
}
