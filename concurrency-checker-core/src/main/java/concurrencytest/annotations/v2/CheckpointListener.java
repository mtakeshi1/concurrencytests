package concurrencytest.annotations.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods with this annotation will be called by the actor thread when it reaches a checkpoint.
 * Contrasting with methods annotated with Invariant, which will be called by a controller thread.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckpointListener {
}
