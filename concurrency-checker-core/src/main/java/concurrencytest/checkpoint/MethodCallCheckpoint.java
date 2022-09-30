package concurrencytest.checkpoint;

import concurrencytest.checkpoint.description.CheckpointDescription;

public interface MethodCallCheckpoint extends CheckpointDescription {

    String methodName();

    Class<?> declaringType();

    Class<?>[] parameterTypes();

    Class<?> returnType();

}
