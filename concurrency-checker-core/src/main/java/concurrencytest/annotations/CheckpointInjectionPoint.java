package concurrencytest.annotations;

public enum CheckpointInjectionPoint {
    ALL,
    FIELDS,
    VOLATILE_FIELD_WRITE,
    ARRAYS,
    SYNCHRONIZED_METHODS,
    SYNCHRONIZED_BLOCKS,
    METHOD_CALL,
    LOCKS,
    ATOMIC_VARIABLES,
    EXCEPTION_THROWN,
    TRY_CATCH_BLOCK

}
