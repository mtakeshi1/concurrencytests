package concurrencytest.runtime.tree;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RuntimeState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CompactingTreeNode extends AbstractHeapTreeNode {

    private final boolean[] shouldProceed;

    public static TreeNode rootNode(Collection<? extends String> actorNames, CheckpointRegister register) {
        return new CompactingTreeNode(null, actorNames.stream().map(actor -> new ActorInformation(actor, register.taskStartingCheckpoint().checkpointId())).collect(Collectors.toMap(ActorInformation::actorName, i -> i)));
    }

    public CompactingTreeNode(TreeNode parent, Map<String, ActorInformation> actorInformationMap) {
        super(parent, actorInformationMap.keySet());
        shouldProceed = new boolean[actorInformationMap.size()];
        int i = 0;
        for (var name : actorNames) {
            var info = Objects.requireNonNull(actorInformationMap.get(name));
            shouldProceed[i++] = !info.finished() && info.isBlocked();
        }
    }

    @Override
    protected boolean shouldExplore(int index) {
        return shouldProceed[index];
    }

    @Override
    public Map<String, ActorInformation> threads() {
        Map<String, ActorInformation> map = new HashMap<>();
        for (var actorName : actorNames) {
            map.put(actorName, new ActorInformation(actorName, 0));
        }
        return map;
    }

    @Override
    public void markFullyExplored() {
        super.markFullyExplored();
        if(super.isFullyExplored()) {
            for(int i = 0; i < actorNames.length; i++) {
                treeNodes[i] = TreeNode.EMPTY_TREE_NODE;
            }
        }
    }

    @Override
    protected TreeNode newChildNode(AbstractHeapTreeNode parent, RuntimeState runtimeState) {
        return new CompactingTreeNode(parent, AbstractHeapTreeNode.collectActorInformations(runtimeState));
    }
}
