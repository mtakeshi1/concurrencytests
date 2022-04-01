package concurrencytest.checkpoint;

public interface FieldCheckpoint extends Checkpoint {

    Class<?> declaringClass();

    String fieldName();

    Class<?> fieldType();

    boolean fieldRead();

    default boolean fieldWrite() {
        return !fieldRead();
    }
}
