package concurrencytest;

import java.util.Collection;

public class DeadlockFoundException extends RuntimeException {

    private final Collection<String> threadNames;
    private final String details;

    public DeadlockFoundException(Collection<String> threadNames, String details) {
        this.threadNames = threadNames;
        this.details = details;
    }


}
