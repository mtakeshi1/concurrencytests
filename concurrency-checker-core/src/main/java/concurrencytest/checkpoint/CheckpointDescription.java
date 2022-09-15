package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public interface CheckpointDescription {

    InjectionPoint injectionPoint();

    String details();

    String sourceFile();

    int lineNumber();

}
