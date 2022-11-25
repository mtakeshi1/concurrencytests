package concurrencytest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method that represents multiple actors.
 * Methods marked with this annotation can receive an 'int' as parameter that represents the actor index.
 *
 * The name of the actors follow the same rule as {@link Actor} except that they are suffixed with the actor index
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Actors {

    Actor[] value();

}
