package sut;

import concurrencytest.annotations.Actor;
import org.junit.After;
import org.junit.Assert;

public class SynchronizedActors {

    private volatile int shared;

    @Actor
    public synchronized void actor1() {
        shared++;
    }

    @Actor
    public synchronized void actor2() {
        shared++;
    }

    @After
    public void check() {
        Assert.assertEquals(2, shared);
    }

}
