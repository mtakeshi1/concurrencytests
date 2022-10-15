package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.annotations.v2.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.runner.RunWith;

//@RunWith(ActorSchedulerRunner.class)
public class SimpleSharedCounter {

    private volatile int counter;

    @ConfigurationSource
    public static Configuration config() {
        return new BasicConfiguration(SimpleSharedCounter.class) {

        };
    }

    @Actor
    public void actor1() {
        counter++;
    }

    @Actor
    public void actor2() {
        counter++;
    }

    @Invariant
    public void invariant() {
        Assert.assertTrue(counter == 0 || counter == 1 || counter == 2);
    }

    @AfterActorsCompleted
    public void check() {
        Assert.assertEquals(2, counter);
    }


}
