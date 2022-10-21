package sut;

import concurrencytest.annotations.Actor;
import org.junit.After;
import org.junit.Assert;

public class RacyIndySynchronizedMethodRef {


    private int value = 0;

    public synchronized void modify() {
        value++;
    }

    private final Runnable modifier = this::modify;

    @Actor
    public void actor1() {
        modifier.run();
    }

    @Actor
    public void actor2() {
        modifier.run();
    }

    @After
    public void check() {
        Assert.assertEquals(2, value);
    }

}
