package sut;

import concurrencytest.annotations.Actor;

public class ExceptionalRacingCondition {

    private final Object monitor = new Object();

    @Actor
    public void errorActor() {
        synchronized (monitor) {
            try {
                throw new RuntimeException("please ignore");
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

}
