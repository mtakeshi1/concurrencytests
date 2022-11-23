package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.runtime.tree.offheap.ByteBufferBackedTreeNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractHeapTreeNode implements TreeNode {
    protected final TreeNode parent;
    //FIXME maybe these needs safer publication?
    protected final String[] actorNames;
    protected final TreeNode[] treeNodes;
    protected final boolean[] startingPoints;
    protected final AtomicBoolean fullyExplored = new AtomicBoolean();

    public AbstractHeapTreeNode(TreeNode parent, Collection<? extends String> names) {
        this.parent = parent;
        this.actorNames = new String[names.size()];
        int i = 0;
        for (var actor : names) {
            actorNames[i++] = actor.intern();
        }
        this.treeNodes = new TreeNode[actorNames.length];
        this.startingPoints = new boolean[actorNames.length];

    }

    public static Map<String, ActorInformation> collectActorInformations(RuntimeState runtimeState) {
        return ByteBufferBackedTreeNode.toActorInformation(runtimeState, runtimeState.actorNamesToThreadStates().values()).stream().collect(Collectors.toMap(ActorInformation::actorName, ai -> ai));
    }

    protected int indexFor(String actorName) {
        for (int i = 0; i < actorNames.length; i++) {
            if (actorNames[i].equals(actorName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("actor named: '%s' not found".formatted(actorName));
    }

    public TreeNode parentNode() {
        return parent;
    }

    public Map<String, Optional<Supplier<TreeNode>>> childNodes() {
        Map<String, Optional<Supplier<TreeNode>>> map = new HashMap<>();
        for (int i = 0; i < actorNames.length; i++) {
            TreeNode node = treeNodes[i];
            if (node != null) {
                map.put(actorNames[i], Optional.of(() -> node));
            } else {
                map.put(actorNames[i], Optional.empty());
            }
        }
        return map;
    }

    public Optional<Supplier<TreeNode>> childNode(String nodeName) {
        TreeNode plainTreeNode = treeNodes[indexFor(nodeName)];
        if (plainTreeNode == null) {
            return Optional.empty();
        }
        return Optional.of(() -> plainTreeNode);
    }

    public Stream<String> unexploredPaths() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < actorNames.length; i++) {
            if ((treeNodes[i] == null || !treeNodes[i].isFullyExplored()) && shouldExplore(i)) {
                list.add(actorNames[i]);
            }
        }
        return list.stream();
    }

    public boolean isFullyExplored() {
        return fullyExplored.get();
    }

    @Override
    public void checkAllChildrenExplored() {
        for (int i = 0; i < actorNames.length; i++) {
            if (!shouldExplore(i)) continue;
            if (treeNodes[i] == null || !treeNodes[i].isFullyExplored()) {
                return;
            }
        }
        markFullyExplored();
    }

    protected abstract boolean shouldExplore(int index);

    public void markFullyExplored() {
        if (fullyExplored.compareAndSet(false, true)) {
            if (parent != null && parent != this) {
                parent.checkAllChildrenExplored();
            }
        }
    }

    public void markLinkAsStartingPoint(String actor) {
        this.startingPoints[indexFor(actor)] = true;
    }

    public boolean isLinkStartingPoint(String link) {
        return startingPoints[indexFor(link)];
    }

    protected abstract TreeNode newChildNode(AbstractHeapTreeNode parent, RuntimeState runtimeState);

    @Override
    public synchronized TreeNode advance(ThreadState selectedToProceed, RuntimeState next) {
        int index = indexFor(selectedToProceed.actorName());
        TreeNode treeNode = treeNodes[index];
        if (treeNode != null) {
            return treeNode;
        }
        return treeNodes[index] = newChildNode(this, next);
    }

}
