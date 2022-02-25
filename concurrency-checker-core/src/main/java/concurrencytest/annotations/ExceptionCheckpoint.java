package concurrencytest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExceptionCheckpoint {

    Class<? extends Throwable> exceptionType() default Throwable.class;

    String exceptionNameRegex() default ".+";

}
