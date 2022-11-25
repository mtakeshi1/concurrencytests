package concurrencytest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shortcut for a method that represent multiple actors, without having to specify the annotation multiple times.
 *
 * Methods with this annotation can receive an 'int' as parameter representing the actor index
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultipleActors {

    int numberOfActors() default 1;

    String actorPreffix() default "";

}
