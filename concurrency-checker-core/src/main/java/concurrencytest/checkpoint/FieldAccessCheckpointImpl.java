package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public class FieldAccessCheckpointImpl extends Checkpoint2 implements FieldAccessCheckpoint {

    private final Class<?> declaringClass;
    private final Class<?> fieldType;
    private final String fieldName;
    private final boolean read;

    public FieldAccessCheckpointImpl(long id, InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber,
                                     Class<?> declaringClass, Class<?> fieldType, String fieldName, boolean read) {
        super(id, injectionPoint, details, sourceFile, lineNumber);
        this.declaringClass = declaringClass;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
        this.read = read;
    }

    @Override
    public Class<?> declaringClass() {
        return declaringClass;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    @Override
    public Class<?> fieldType() {
        return fieldType;
    }

    @Override
    public boolean fieldRead() {
        return read;
    }
}
