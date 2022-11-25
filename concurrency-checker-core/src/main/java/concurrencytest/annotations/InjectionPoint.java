package concurrencytest.annotations;

/**
 * Indicates wheter or not a checkpoint is before or after an injection point. For instance, if the checkpoint is at a method invocation,
 * the BEFORE injection point is injected right before the method call.
 */
public enum InjectionPoint {

    BEFORE, AFTER

}
