package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.TestParameters;

import static concurrencytest.annotations.CheckpointInjectionPoint.*;

@TestParameters(defaultCheckpoints = {VOLATILE_FIELD_WRITE,SYNCHRONIZED_METHODS, SYNCHRONIZED_BLOCKS, LOCKS, ATOMIC_VARIABLES})
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
