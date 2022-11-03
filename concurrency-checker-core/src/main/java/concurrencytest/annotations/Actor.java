package concurrencytest.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Actors.class)
public @interface Actor {

    String actorName() default "";

}
