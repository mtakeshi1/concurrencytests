package concurrencytest.runtime.tree;

import java.util.Optional;

public record LockOrMonitorInformation(String monitorType, Optional<String> ownerName, String source, int lineNumber) {

}
