package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
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
//        Assert.assertNotNull(shared);
    }

    @Invariant
    public void sharedIsNeverNegative() {
//        Assert.assertTrue(shared.getValue() >= 0);
    }

    @Actor
    public void actor1() {
        shared.setValue(shared.getValue() + 1);
    }

    @Actor
    public void actor2() {
        shared.setValue(shared.getValue() + 1);
    }

    @After
    public void check() {
        Assert.assertEquals(2, shared.getValue());
    }

}
