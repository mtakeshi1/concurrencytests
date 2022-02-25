package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.TestParameters;
import org.junit.After;
import org.junit.Assert;

@TestParameters(threadTimeoutSeconds = 1, runTimeoutSeconds = 2)
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
