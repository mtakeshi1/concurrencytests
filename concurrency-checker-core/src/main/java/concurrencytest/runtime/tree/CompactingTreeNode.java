package concurrencytest.runtime.tree;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.runtime.tree.offheap.ByteBufferBackedTreeNode;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompactingTreeNode extends PlainTreeNode {

    public static TreeNode rootNode(Collection<? extends String> actorNames, CheckpointRegister register) {
        return new CompactingTreeNode(null, actorNames.stream().map(actor -> new ActorInformation(actor, register.taskStartingCheckpoint().checkpointId())).collect(Collectors.toMap(ActorInformation::actorName, i -> i)));
    }

    public static CompactingTreeNode newChildNode(CompactingTreeNode parent, RuntimeState runtimeState) {
        Map<String, ActorInformation> map = ByteBufferBackedTreeNode.toActorInformation(runtimeState, runtimeState.actorNamesToThreadStates().values()).stream().collect(Collectors.toMap(ActorInformation::actorName, ai -> ai));
        return new CompactingTreeNode(parent, map);
    }

    public CompactingTreeNode(CompactingTreeNode parent, Map<String, ActorInformation> actorInformationMap) {
        super(parent, actorInformationMap);
    }

    @Override
    public void markFullyExplored() {
        super.markFullyExplored();
        if (fullyExplored) {
            for(int i = 0; i < actorNames.length; i++) {
                //FIXME maybe set the arrays to null instead, but that would require making them mutable
                actorInformations[i] = null;
                actorNames[i] = null;
            }
//            super.nodes.clear();
//            super.actorInformationMap.clear();
//            super.startingPoints.clear();
        }
    }
}
