package concurrencytest.checkpoint;

public interface FieldAccessCheckpoint extends Checkpoint {

    Class<?> declaringClass();

    String fieldName();

    Class<?> fieldType();

    boolean fieldRead();

    default boolean fieldWrite() {
        return !fieldRead();
    }
}
