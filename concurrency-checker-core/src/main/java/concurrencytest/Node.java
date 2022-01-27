package concurrencytest;

import java.util.*;

public class Node {
//    private volatile RuntimeState state; //remove this ref
//    private final Map<String, Node> links = new HashMap<>();

    private volatile Node[] links;
    private volatile String[] linkNames;

    private static final String[] EMPTY_NAMES = new String[0];
    private static final Node[] EMPTY_NODES = new Node[0];

    private volatile boolean terminal;

    private long sizeCached = 0;

    public Node(RuntimeState state) {
//        this.state = state;
        ThreadState[] canAdvance = state.getCurrentState().values().stream().filter(ts -> ts.isRunnable() && ts.isAlive()).toArray(ThreadState[]::new);

        if (canAdvance.length == 0) {
            linkNames = EMPTY_NAMES;
            links = EMPTY_NODES;
        } else {
            this.linkNames = new String[canAdvance.length];
            for (int i = 0; i < canAdvance.length; i++) {
                linkNames[i] = canAdvance[i].getActorIdentification();
            }
            Arrays.sort(linkNames);
            this.links = new Node[linkNames.length];
        }
    }

    public void markTerminal() {
        terminal = true;
    }

    private int indexOf(String threadName) {
        String[] localLinkNames = this.linkNames;
        if (localLinkNames == null) {
            return -1;
        }
        int i = Arrays.binarySearch(localLinkNames, threadName);
        if (i < 0) {
            throw new RuntimeException("Thread with name: " + threadName + " not found on " + Arrays.toString(localLinkNames));
        }
        return i;
    }


    public synchronized Node linkTo(String actorIdentification, Node neighboor) {
        if (Arrays.equals(this.linkNames, neighboor.linkNames)) {
            neighboor.linkNames = this.linkNames;
        }
        int index = indexOf(actorIdentification);
        if(index < 0) {
            return null;
        }
        if (links[index] != null) {
            return links[index];
        }
        return links[index] = neighboor;
    }

    public Node findNeighboor(String path) {
        return links[indexOf(path)];
    }

    public boolean hasUnvisitedState() {
        if (terminal) {
            return false;
        }
        boolean b = findNextThread(null, false) != null;
        if (!b) {
            terminal = true;
            links = null;
            linkNames = null;
        }
        return b;
    }

    public int getWidth() {
        int i = 0;
        for (Node n : links) {
            if (n != null) i++;
        }
        return i;
    }

    public Map<String, Node> getLinks() {
        Map<String, Node> map = new HashMap<>();
        for (int i = 0; i < links.length; i++) {
            if (links[i] != null) {
                map.put(linkNames[i], links[i]);
            }
        }
        return map;
    }

    public boolean containsLinkWith(String threadName) {
        int i = Arrays.binarySearch(linkNames, threadName);
        return i >= 0 && links[i] != null;
    }

    public synchronized String findNextThread(Collection<String> allowed, boolean randomPick) {
        if (terminal || linkNames == null || links == null) {
            return null;
        }
        //TODO consider loop detection, prioritize threads with lower loop count
        if (randomPick) {
            Set<String> allAllowed = collectAllowedThreads(allowed);
            if (allAllowed.isEmpty()) {
                return null;
            }
            String[] strings = allAllowed.toArray(new String[allAllowed.size()]);
            int index = new Random().nextInt(strings.length);
            return strings[index];
        } else {
            for (int i = 0; i < linkNames.length; i++) {
                if (links[i] != null && links[i].hasUnvisitedState() && (allowed == null || allowed.contains(linkNames[i]))) {
                    return linkNames[i];
                }
            }
            for (int i = 0; i < linkNames.length; i++) {
                if (links[i] == null && (allowed == null || allowed.contains(linkNames[i]))) {
                    return linkNames[i];
                }
            }
            return null;
        }
    }

    public synchronized Set<String> collectAllowedThreads(Collection<String> allowed) {
        Set<String> allAllowed = new HashSet<>();
        for (int i = 0; i < linkNames.length; i++) {
            if ((links[i] == null || (links[i] != null && links[i].hasUnvisitedState())) && (allowed == null || allowed.contains(linkNames[i]))) {
                allAllowed.add(linkNames[i]);
            }
        }
        return allAllowed;
    }

    public long size() {
        long x = 1;
        Node[] localNodes = this.links;
        if (localNodes == null) {
            return sizeCached;
        }
        for (int i = 0; i < localNodes.length; i++) {
            if (localNodes[i] != null) {
                x += localNodes[i].size();
            }
        }
        return sizeCached = x ;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
