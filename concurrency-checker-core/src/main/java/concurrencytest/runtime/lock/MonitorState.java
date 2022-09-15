package concurrencytest.runtime.lock;

import concurrencytest.runtime.ManagedThread;

import java.util.ArrayList;
import java.util.List;

public class MonitorState {

    private final int identityHashCode;

    private List<ManagedThread> waitingForMonitor = new ArrayList<>();

    private ManagedThread currentlyHolding;

    public MonitorState(int identityHashCode) {
        this.identityHashCode = identityHashCode;
    }


}
