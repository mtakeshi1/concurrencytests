package concurrencytest.runtime.tree;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.ThreadState;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlainTreeNode implements TreeNode {

    private final PlainTreeNode parent;
    private final Map<String, ActorInformation> actorInformationMap;
    private final ConcurrentMap<String, PlainTreeNode> nodes = new ConcurrentHashMap<>();
    private volatile boolean fullyExplored;

    public static TreeNode rootNode(Collection<? extends String> actorNames, CheckpointRegister register) {
        return new PlainTreeNode(null, actorNames.stream().map(actor -> new ActorInformation(actor, register.taskStartingCheckpoint().checkpointId())).collect(Collectors.toMap(ActorInformation::actorName, i -> i)));
    }

    public static PlainTreeNode newChildNode(PlainTreeNode parent, RuntimeState runtimeState) {
        Map<String, ActorInformation> map = ByteBufferBackedTreeNode.toActorInformation(runtimeState.actorNamesToThreadStates().values()).stream().collect(Collectors.toMap(ActorInformation::actorName, ai -> ai));
        return new PlainTreeNode(parent, map);
    }

    public PlainTreeNode(PlainTreeNode parent, Map<String, ActorInformation> actorInformationMap) {
        this.parent = parent;
        this.actorInformationMap = actorInformationMap;
    }

    @Override
    public TreeNode parentNode() {
        return parent;
    }

    @Override
    public Map<String, ActorInformation> threads() {
        return actorInformationMap;
    }

    @Override
    public Map<String, Optional<Supplier<TreeNode>>> childNodes() {
        return actorInformationMap.keySet().stream().collect(Collectors.toMap(ac -> ac, this::childNode));
    }

    @Override
    public Optional<Supplier<TreeNode>> childNode(String nodeName) {
        PlainTreeNode plainTreeNode = nodes.get(nodeName);
        if (plainTreeNode == null) {
            return Optional.empty();
        }
        return Optional.of(() -> plainTreeNode);
    }

    private boolean shouldExplore(String actorName) {
        ActorInformation information = actorInformationMap.get(actorName);
        Objects.requireNonNull(information, "Path not found for actor named: %s".formatted(actorName));
        PlainTreeNode link = nodes.get(actorName);
        return !information.isBlocked() && (link == null || !link.isFullyExplored());
    }

    @Override
    public Stream<String> unexploredPaths() {
        return actorInformationMap.keySet().stream().filter(this::shouldExplore);
    }

    @Override
    public TreeNode advance(ThreadState selectedToProceed, RuntimeState next) {
        return nodes.computeIfAbsent(selectedToProceed.actorName(), actorName -> newChildNode(this, next));
    }

    @Override
    public boolean isFullyExplored() {
        return fullyExplored;
    }

    @Override
    public void markFullyExplored() {
        fullyExplored = true;
    }
}
