package concurrencytest.annotations;

public enum CheckpointInjectionPoint {
    ALL,
    FIELDS,
    VOLATILE_FIELDS,
    ARRAYS,
    SYNCHRONIZED_METHODS,
    SYNCHRONIZED_BLOCKS,
    METHOD_CALL,
    LOCKS,
    ATOMIC_VARIABLES,
    EXCEPTION_THROWN,
    TRY_CATCH_BLOCK,
    MANUAL

}
