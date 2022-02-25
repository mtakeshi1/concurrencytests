package concurrencytest.annotations;

import java.lang.annotation.*;

@Repeatable(value = ManualCheckpoints.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ManualCheckpoint {

    FieldCheckpoint[] fieldCheckpoints() default {};

    MethodCallCheckpoint[] methodCheckpoints() default {};

    LineNumberCheckpoint[] lineCheckpoints() default {};

//    ExceptionCheckpoint[] exceptionCheckpoints() default {};

}
