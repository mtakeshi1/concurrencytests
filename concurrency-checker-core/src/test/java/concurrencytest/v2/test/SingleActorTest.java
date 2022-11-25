package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.AfterActorsCompleted;
import org.junit.Assert;

public class SingleActorTest {

    private volatile int counter;

    @Actor
    public void actor1() {
        counter++;
    }

    @Invariant
    public void counterBounds() {
        Assert.assertTrue(counter >= 0 && counter <= 1);
    }

    @AfterActorsCompleted
    public void afterFinish() {
        Assert.assertEquals(1, counter);
    }

}
