package concurrencytest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FieldCheckpoint {
    Class<?> declaringClass() default Object.class;

    String declaringClassNameRegex() default ".+";

    InjectionPoint[] points() default {InjectionPoint.AFTER};

    String fieldNameRegex();

    boolean fieldRead() default false;

    boolean fieldWrite() default true;

}
