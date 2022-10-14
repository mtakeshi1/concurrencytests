package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;

public class TwoActorsVolatileRead {

    private volatile int counter;

    @Actor
    public void actor1() {
        System.out.println(counter);
    }

    @Actor
    public void actor2() {
        System.out.println(counter);
    }


}
