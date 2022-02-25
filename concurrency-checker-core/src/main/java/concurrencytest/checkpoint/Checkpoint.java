package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public interface Checkpoint {

    long checkpointId();

    InjectionPoint injectionPoint();

    String details();

    String sourceFile();

    int lineNumber();

}
