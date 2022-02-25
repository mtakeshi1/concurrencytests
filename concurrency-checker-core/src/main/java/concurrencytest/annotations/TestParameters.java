package concurrencytest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestParameters {

    boolean autodetectClasses() default true;

    InstrumentationStrategy instrumentationStrategy() default InstrumentationStrategy.RENAME;

    Class<?>[] instrumentedClasses() default {};

    int maxLoopCount() default 100;

    int threadTimeoutSeconds() default 10;

    int runTimeoutSeconds() default 10;

    Class<?>[] excludedClasses() default {};

    boolean randomPick() default false;

    int parallelScenarios() default -1;

    CheckpointInjectionPoint[] defaultCheckpoints() default {
            CheckpointInjectionPoint.VOLATILE_FIELD_WRITE, CheckpointInjectionPoint.ARRAYS, CheckpointInjectionPoint.SYNCHRONIZED_METHODS,
            CheckpointInjectionPoint.SYNCHRONIZED_BLOCKS, CheckpointInjectionPoint.ATOMIC_VARIABLES, CheckpointInjectionPoint.LOCKS
    };

}
