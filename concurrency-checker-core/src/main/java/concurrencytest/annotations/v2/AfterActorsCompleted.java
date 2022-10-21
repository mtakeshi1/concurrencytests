package concurrencytest.annotations.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods annotated with this annotation are called to check for end-of-run invariants when all the actors finish without errors.
 * On the other hand, methods annotated with After will be used for cleanup, even if a run ends prematurely (eg the engine realizes that all
 * paths have been explored) or if some invariant fails
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterActorsCompleted {
}
