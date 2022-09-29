package concurrencytest.checkpoint;

public interface FieldAccessCheckpoint extends CheckpointDescription {

    String declaringClass();

    String fieldName();

    String fieldType();

    boolean fieldRead();

    default boolean fieldWrite() {
        return !fieldRead();
    }
}
