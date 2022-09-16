package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointRegister;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public interface RuntimeState {

    CheckpointRegister register();

    int monitorIdFor(Object object);

    int lockIdFor(Lock lock);

    Map<Integer, ManagedThread> ownedMonitors();

    Map<Integer, Collection<ManagedThread>> threadsWaitingForMonitor();

    Map<Integer, ManagedThread> lockedLocks();

    Map<Integer, Collection<ManagedThread>> threadsWaitingForLocks();

    Map<ManagedThread, CheckpointReached> threadStates();


}
