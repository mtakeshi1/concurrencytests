package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class RacyActorsGetters {

    private SynchronizedValueHolder shared;

    @Before
    public void before() {
        shared = new SynchronizedValueHolder();
    }

    @Invariant
    public void sharedIsNeverNull() {
        Assert.assertNotNull(shared);
    }

    @Invariant
    public void sharedIsNeverNegative() {
        // the following line deadlocks the test runtime
//        Assert.assertTrue(shared.getValue() >= 0);
    }

    @Actor
    public void actor1() {
        int value = shared.getValue();
        System.out.println("actor1 read: " + value);
        shared.setValue(value + 1);
    }

    @Actor
    public void actor2() {
        int value = shared.getValue();
        System.out.println("actor2 read: " + value);
        shared.setValue(value + 1);
    }

    @AfterActorsCompleted
    public void check() {
        Assert.assertEquals(2, shared.getValue());
    }

}
