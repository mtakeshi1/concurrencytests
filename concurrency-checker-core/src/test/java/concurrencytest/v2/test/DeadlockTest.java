package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.AfterActorsCompleted;
import org.junit.Assert;

public class DeadlockTest {

    private final Object left = new Object(), right = new Object();

    private volatile int counter;

    @Actor
    public void actor1() {
        synchronized (left) {
            synchronized (right) {
                counter++;
            }
        }
    }

    @Actor
    public void actor2() {
        synchronized (right) {
            synchronized (left) {
                counter++;
            }
        }
    }

    @AfterActorsCompleted
    public void finished() {
        Assert.assertEquals(2, counter);
    }

}
