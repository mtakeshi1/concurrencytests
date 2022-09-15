package concurrencytest;

import concurrencytest.checkpoint.OldCheckpointImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ExecutionGraph {

    // TODO state -> List<Node>
    // checkpoint cache
    // loop detection

    private Node initialNode;

    private final Map<Long, OldCheckpointImpl> checkpointMap = new ConcurrentHashMap<>();

    public OldCheckpointImpl computeCheckpointIfAbsent(long id, Function<Long, OldCheckpointImpl> generator) {
        return checkpointMap.computeIfAbsent(id, generator);
    }

    public synchronized Node reachedState(List<String> pathTaken, RuntimeState currentThreadStates) {
        if (pathTaken.isEmpty()) {
            if (initialNode == null) {
                this.initialNode = new Node(currentThreadStates);
            }
            return initialNode;
        } else {
            Node visiting = initialNode;
            for (int i = 0; i < pathTaken.size() - 1; i++) {
                var path = pathTaken.get(i);
                Node toVisit = visiting.findNeighboor(path);
                if (toVisit == null) {
                    throw new RuntimeException("Could not find link from node: " + visiting + " with path: " + path);
                }
                visiting = toVisit;
            }
            String lastPiece = pathTaken.get(pathTaken.size() - 1);
            return visiting.linkTo(lastPiece, new Node(currentThreadStates));
        }
    }


    public synchronized boolean hasUnvisitedState() {
        return this.initialNode.hasUnvisitedState();
    }

    public void markTerminal(RuntimeState state) {
        throw new RuntimeException("TODO");
    }

//    public synchronized ManagedThread findNextThread(List<String> pathTaken, Map<String, ManagedThread> canAdvance) {
//        Node visiting = walkNodes(pathTaken);
//        String nextThread = visiting.findNextThread(canAdvance.keySet());
//        if (nextThread != null) {
//            return canAdvance.get(nextThread);
//        }
//        return null;
//    }

    private Node walkNodes(List<String> pathTaken) {
        Node visiting = initialNode;
        for (String s : pathTaken) {
            Node toVisit = visiting.findNeighboor(s);
            if (toVisit == null) {
                throw new RuntimeException("Could not find link from node: " + visiting + " with path: " + s);
            }
            visiting = toVisit;
        }
        return visiting;
    }

    public OldCheckpointImpl getExistingCheckpont(long id) {
        OldCheckpointImpl checkpoint = this.checkpointMap.get(id);
        if (checkpoint == null) {
            throw new RuntimeException("CheckpointImpl with id: " + id + " not found");
        }
        return checkpoint;
    }

    public int getInitialNodeWidth() {
        if (initialNode == null) {
            return 0;
        }
        return initialNode.getWidth();
    }

    public long size() {
        if (initialNode == null) {
            return 0L;
        }
        return initialNode.size();
    }

    public Node getInitialNode() {
        return initialNode;
    }

    public Node walk(Collection<String> preffix) {
        Node node = this.initialNode;
        for (String path : preffix) {
            if (node == null) {
                return null;
            }
            if (node.isTerminal()) {
                return node;
            }
            node = node.findNeighboor(path);
            if (node == null) {
                return null;
            }
        }
        return node;
    }
}
