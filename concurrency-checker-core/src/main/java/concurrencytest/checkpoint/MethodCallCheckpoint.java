package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public interface MethodCallCheckpoint extends Checkpoint {

    InjectionPoint injectionPoint();

    String methodName();

    Class<?> declaringType();

    Class<?>[] parameterTypes();

    Class<?> returnType();

}
