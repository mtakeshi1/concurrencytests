package concurrencytest.checkpoint.description;

import concurrencytest.checkpoint.description.CheckpointDescription;

public interface MethodCallCheckpointDescription extends CheckpointDescription {

    String methodName();

    Class<?> declaringType();

    Class<?>[] parameterTypes();

    Class<?> returnType();

}
