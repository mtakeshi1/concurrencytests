package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public abstract class Checkpoint2 implements Checkpoint {

    private final long id;
    private final InjectionPoint injectionPoint;

    private final String details, sourceFile;
    private final int lineNumber;

    public Checkpoint2(long id, InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber) {
        this.id = id;
        this.injectionPoint = injectionPoint;
        this.details = details;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    @Override
    public long checkpointId() {
        return id;
    }

    @Override
    public InjectionPoint injectionPoint() {
        return injectionPoint;
    }

    @Override
    public String details() {
        return details;
    }

    @Override
    public String sourceFile() {
        return sourceFile;
    }

    @Override
    public int lineNumber() {
        return lineNumber;
    }
}
