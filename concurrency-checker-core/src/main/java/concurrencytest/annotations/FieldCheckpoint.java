package concurrencytest.annotations;

import concurrencytest.asm.AccessModifier;
import concurrencytest.asm.BehaviourModifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure field injection points. Currently not being used by the runner.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FieldCheckpoint {
    Class<?> declaringClass() default Object.class;

    String declaringClassNameRegex() default ".+";

    String fieldNameRegex();

    //TODO maybe have a field type specifier?

    InjectionPoint[] injectionPoints() default {InjectionPoint.AFTER};

    AccessModifier[] accessModifiers() default {AccessModifier.PUBLIC, AccessModifier.PROTECTED, AccessModifier.DEFAULT, AccessModifier.PRIVATE};

    BehaviourModifier[] behaviourModifiers() default {BehaviourModifier.STATIC, BehaviourModifier.SYNCHRONIZED, BehaviourModifier.VOLATILE, BehaviourModifier.TRANSIENT, BehaviourModifier.INSTANCE_MEMBER};

    boolean fieldRead() default false;

    boolean fieldWrite() default true;

}
