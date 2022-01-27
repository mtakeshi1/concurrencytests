package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.TestParameters;

import static concurrencytest.annotations.CheckpointInjectionPoint.*;

@TestParameters(injectionPoints = {FIELDS, VOLATILE_FIELDS, ARRAYS, SYNCHRONIZED_METHODS, SYNCHRONIZED_BLOCKS, METHOD_CALL, LOCKS, ATOMIC_VARIABLES, MANUAL})
public class ExceptionalRacingCondition {

    private final Object monitor = new Object();

    @Actor
    public void errorActor() {
        synchronized (monitor) {
            try {
                throw new RuntimeException("bla");
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

}
