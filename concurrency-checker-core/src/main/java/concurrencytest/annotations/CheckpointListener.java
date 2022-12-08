package concurrencytest.annotations;

import concurrencytest.checkpoint.instance.CheckpointReached;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks methods that will be called inside the test class to listen for actors reaching checkpoints.
 * The method must be public, non-static and can receive as arguments the following types:
 * - int - the checkpoint id
 * - {@link concurrencytest.runtime.thread.ManagedThread} the thread reaching the checkpoint
 * - String - the actor name
 * - {@link CheckpointReached}
 *
 * Currently not implemented
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckpointListener {
}
