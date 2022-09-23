package concurrencytest.runtime.tree;

import concurrencytest.runtime.ManagedThread;
import concurrencytest.runtime.RuntimeState;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public record ThreadState(String actorName, int checkpoint, int loopCount, List<Integer> ownedMonitors,
                          Optional<Integer> waitingForMonitor, List<Integer> ownedLocks,
                          Optional<Integer> waitingForLock,
                          Optional<ManagedThread> waitingForThread, ManagedThread managedThread) {

    public boolean canProceed(RuntimeState state) {
        return monitorIsFreeOrMine(state)
                && lockIsFreeOrMine(state)
                && waitingForThread.isEmpty();
    }

    public boolean lockIsFreeOrMine(RuntimeState state) {
        return waitingForMonitor.map(checkpointId1 -> state.ownedMonitors().get(checkpointId1) == null || state.lockedLocks().get(checkpointId1) == this.managedThread()).orElse(true);
    }

    public boolean monitorIsFreeOrMine(RuntimeState state) {
        return waitingForMonitor.map(checkpointId -> state.ownedMonitors().get(checkpointId) == null || state.ownedMonitors().get(checkpointId) == this.managedThread()).orElse(true);
    }

}
