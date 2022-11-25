package concurrencytest.annotations;

import java.lang.annotation.*;

/**
 * Methods marked with this annotation are the starting point of threads to be run.
 *
 * The parameter {@link Actor#actorName()} is used to identify the actor. If its blank, it will be named after the method name
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Actors.class)
public @interface Actor {

    String actorName() default "";

}
