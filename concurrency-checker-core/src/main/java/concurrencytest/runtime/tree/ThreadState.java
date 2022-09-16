package concurrencytest.runtime.tree;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public record ThreadState(String name, int checkpoint, int loopCount, List<Integer> ownedMonitors,
                          OptionalInt waitingForMonitor, List<Integer> ownedLocks, OptionalInt waitingForLock,
                          Thread platformThread, boolean runnable) {

}
