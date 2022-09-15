package concurrencytest.checkpoint;

public interface MethodCallCheckpoint extends CheckpointDescription {

    String methodName();

    Class<?> declaringType();

    Class<?>[] parameterTypes();

    Class<?> returnType();

}
