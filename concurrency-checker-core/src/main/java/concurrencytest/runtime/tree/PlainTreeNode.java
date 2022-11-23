package concurrencytest.runtime.tree;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RuntimeState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlainTreeNode extends AbstractHeapTreeNode implements TreeNode {

    protected final ActorInformation[] actorInformations;

    public static TreeNode rootNode(Collection<? extends String> actorNames, CheckpointRegister register) {
        return new PlainTreeNode(null, actorNames.stream().map(actor -> new ActorInformation(actor, register.taskStartingCheckpoint().checkpointId())).collect(Collectors.toMap(ActorInformation::actorName, i -> i)));
    }

    public PlainTreeNode(TreeNode parent, Map<String, ActorInformation> actorInformationMap) {
        super(parent, actorInformationMap.keySet());
        this.actorInformations = new ActorInformation[actorNames.length];
        int i = 0;
        for (var entry : actorInformationMap.entrySet()) {
            actorInformations[i++] = entry.getValue();
        }
    }

    protected TreeNode newChildNode(AbstractHeapTreeNode parent, RuntimeState runtimeState) {
        return new PlainTreeNode(parent, collectActorInformations(runtimeState));
    }

    @Override
    public Map<String, ActorInformation> threads() {
        Map<String, ActorInformation> map = new HashMap<>();
        for (int i = 0; i < actorNames.length; i++) {
            map.put(actorNames[i], actorInformations[i]);
        }
        return map;
    }

    protected boolean shouldExplore(int index) {
        return !Objects.requireNonNull(actorInformations[index]).isBlocked() &&
                !Objects.requireNonNull(actorInformations[index]).finished() &&
                !startingPoints[index] &&
                (treeNodes[index] == null || !treeNodes[index].isFullyExplored());
    }

}
