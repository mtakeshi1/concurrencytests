package concurrencytest.runtime.tree;

import java.util.List;
import java.util.Optional;

public record ActorInformation(String actorName, List<LockOrMonitorInformation> monitorsOwned, List<LockOrMonitorInformation> locksLocked,
                               Optional<LockOrMonitorInformation> waitingForMonitor, Optional<LockOrMonitorInformation> waitingForLock,
                               boolean finished) {


}
