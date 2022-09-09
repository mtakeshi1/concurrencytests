package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public interface Checkpoint {

    int checkpointId();

    InjectionPoint injectionPoint();

    String details();

    String sourceFile();

    int lineNumber();

}
