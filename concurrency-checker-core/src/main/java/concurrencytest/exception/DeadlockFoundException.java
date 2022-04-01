package concurrencytest.exception;

import java.util.Collection;
import java.util.Collections;

public class DeadlockFoundException extends RuntimeException {

    private final Collection<String> threadNames;
    private final String details;

    public DeadlockFoundException(Collection<String> threadNames, String details) {
        this.threadNames = Collections.unmodifiableCollection(threadNames);
        this.details = details;
    }

    public Collection<String> getThreadNames() {
        return threadNames;
    }

    public String getDetails() {
        return details;
    }
}
