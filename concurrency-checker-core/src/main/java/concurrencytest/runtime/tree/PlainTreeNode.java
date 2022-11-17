package concurrencytest.runtime.tree;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.runtime.tree.offheap.ByteBufferBackedTreeNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlainTreeNode implements TreeNode {

    private final PlainTreeNode parent;
//    protected final Map<String, ActorInformation> actorInformationMap;
//    protected final ConcurrentMap<String, TreeNode> nodes = new ConcurrentHashMap<>();
//    protected final ConcurrentMap<String, Boolean> startingPoints = new ConcurrentHashMap<>();

    //FIXME maybe these needs safer publication?
    protected final String[] actorNames;
    protected final ActorInformation[] actorInformations;
    protected final TreeNode[] treeNodes;
    private final boolean[] startingPoints;

    protected final AtomicBoolean fullyExplored = new AtomicBoolean();

    public static TreeNode rootNode(Collection<? extends String> actorNames, CheckpointRegister register) {
        return new PlainTreeNode(null, actorNames.stream().map(actor -> new ActorInformation(actor, register.taskStartingCheckpoint().checkpointId())).collect(Collectors.toMap(ActorInformation::actorName, i -> i)));
    }

    public static PlainTreeNode newChildNode(PlainTreeNode parent, RuntimeState runtimeState) {
        Map<String, ActorInformation> map = ByteBufferBackedTreeNode.toActorInformation(runtimeState, runtimeState.actorNamesToThreadStates().values()).stream().collect(Collectors.toMap(ActorInformation::actorName, ai -> ai));
        return new PlainTreeNode(parent, map);
    }

    public PlainTreeNode(PlainTreeNode parent, Map<String, ActorInformation> actorInformationMap) {
        this.parent = parent;
        this.actorNames = new String[actorInformationMap.size()];
        this.actorInformations = new ActorInformation[actorNames.length];
        this.startingPoints = new boolean[actorNames.length];
        this.treeNodes = new TreeNode[actorNames.length];
        int i = 0;
        for (var entry : actorInformationMap.entrySet()) {
            actorNames[i] = entry.getKey();
            actorInformations[i++] = entry.getValue();
        }
    }

    protected int indexFor(String actorName) {
        for (int i = 0; i < actorNames.length; i++) {
            if (actorNames[i].equals(actorName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("actor named: '%s' not found".formatted(actorName));
    }

    @Override
    public TreeNode parentNode() {
        return parent;
    }

    @Override
    public Map<String, ActorInformation> threads() {
        Map<String, ActorInformation> map = new HashMap<>();
        for (int i = 0; i < actorNames.length; i++) {
            map.put(actorNames[i], actorInformations[i]);
        }
        return map;
    }

    @Override
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

    @Override
    public Optional<Supplier<TreeNode>> childNode(String nodeName) {
        TreeNode plainTreeNode = treeNodes[indexFor(nodeName)];
        if (plainTreeNode == null) {
            return Optional.empty();
        }
        return Optional.of(() -> plainTreeNode);
    }

    //    private boolean shouldExplore(String actorName) {
//        ActorInformation information = actorInformationMap.get(actorName);
//        Objects.requireNonNull(information, "Path not found for actor named: %s".formatted(actorName));
//        TreeNode link = nodes.get(actorName);
//        return !information.isBlocked() && (link == null || !link.isFullyExplored()) && !information.finished();
//    }
    @Override
    public Stream<String> unexploredPaths() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < actorNames.length; i++) {
            var info = Objects.requireNonNull(actorInformations[i]);
            var node = treeNodes[i];
            if (shouldExplore(i)) {
                list.add(actorNames[i]);
            }
        }
        return list.stream();
    }

    @Override
    public synchronized TreeNode advance(ThreadState selectedToProceed, RuntimeState next) {
        int index = indexFor(selectedToProceed.actorName());
        TreeNode treeNode = treeNodes[index];
        if (treeNode != null) {
            return treeNode;
        }
        return treeNodes[index] = newChildNode(this, next);
    }

    @Override
    public boolean isFullyExplored() {
        return fullyExplored.get();
    }

    @Override
    public void checkAllChildrenExplored() {
        for (int i = 0; i < actorNames.length; i++) {
            if (actorInformations[i].finished()) continue;
            if (actorInformations[i].isBlocked()) continue;
            if (treeNodes[i] == null || !treeNodes[i].isFullyExplored()) {
                return;
            }
        }
        markFullyExplored();
    }

    private boolean shouldExplore(int index) {
        return !Objects.requireNonNull(actorInformations[index]).isBlocked() &&
                !Objects.requireNonNull(actorInformations[index]).finished() &&
                !startingPoints[index] &&
                (treeNodes[index] == null || !treeNodes[index].isFullyExplored());
    }

    @Override
    public void markFullyExplored() {
        if (fullyExplored.compareAndSet(false, true)) {
            if (parent != null && parent != this) {
                parent.checkAllChildrenExplored();
            }
        }
    }

    @Override
    public void markLinkAsStartingPoint(String actor) {
        this.startingPoints[indexFor(actor)] = true;
    }

    @Override
    public boolean isLinkStartingPoint(String link) {
        return startingPoints[indexFor(link)];
    }
}
