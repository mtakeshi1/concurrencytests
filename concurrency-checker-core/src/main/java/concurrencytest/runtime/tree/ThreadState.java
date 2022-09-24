package concurrencytest.runtime.tree;

import concurrencytest.runtime.ManagedThread;
import concurrencytest.runtime.RuntimeState;

import java.util.*;
import java.util.stream.Stream;

public record ThreadState(String actorName, int checkpoint, int loopCount, List<Integer> ownedMonitors,
                          Optional<Integer> waitingForMonitor, List<Integer> ownedLocks,
                          Optional<Integer> waitingForLock, Optional<String> waitingForThread, boolean finished) {

    public ThreadState(String actorName) {
        this(actorName, -1, 0, Collections.emptyList(), Optional.empty(), Collections.emptyList(), Optional.empty(), Optional.empty(), false);
    }

    public boolean runnable() {
        return !finished();
    }

    public Collection<ManagedThread> dependencies(RuntimeState state) {
        Stream<ManagedThread> monitor = waitingForMonitor.filter(monId -> !state.ownedMonitors().containsKey(monId)).map(mon -> state.ownedMonitors().get(mon)).stream();
        Stream<ManagedThread> locks = waitingForLock.filter(monId -> !state.lockedLocks().containsKey(monId)).map(mon -> state.lockedLocks().get(mon)).stream();
        Stream<ManagedThread> conditionalWait = waitingForThread.map(actor -> state.actorNamesToThreads().get(actor)).stream();
        return Stream.concat(monitor, Stream.concat(locks, conditionalWait)).toList();
    }

    public boolean canProceed(RuntimeState state) {
        return monitorIsFreeOrMine(state)
                && lockIsFreeOrMine(state)
                && waitingForThread.isEmpty();
    }

    public boolean lockIsFreeOrMine(RuntimeState state) {
        return waitingForMonitor.map(checkpointId1 -> state.ownedMonitors().get(checkpointId1) == null ||
                state.lockedLocks().get(checkpointId1).getActorName().equals(actorName())).orElse(true);
    }

    public boolean monitorIsFreeOrMine(RuntimeState state) {
        return waitingForMonitor.map(checkpointId -> state.ownedMonitors().get(checkpointId) == null || state.ownedMonitors().get(checkpointId).getActorName().equals(this.actorName())).orElse(true);
    }

}
