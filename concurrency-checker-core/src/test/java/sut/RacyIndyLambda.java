package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import org.junit.After;
import org.junit.Assert;

public class RacyIndyLambda {


    private volatile int value = 0;

    private final Runnable modifier = () -> value++;

    @Actor
    public void actor1() {
        modifier.run();
    }

    @Actor
    public void actor2() {
        modifier.run();
    }

    @AfterActorsCompleted
    public void check() {
        Assert.assertEquals(2, value);
    }

}
