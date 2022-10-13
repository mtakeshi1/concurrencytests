package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(ActorSchedulerRunner.class)
public class SingularActorTest {

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
