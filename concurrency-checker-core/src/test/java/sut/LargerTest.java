package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

//@RunWith(ConcurrencyRunner.class)
@Ignore
public class LargerTest {


    public static final int MAX = 1;
    private final AtomicInteger counter = new AtomicInteger();

    @Actor
    public void compareAndSet1() {
        for (int i = 0; i < MAX; i++) {
            while (true) {
                int x = counter.get();
                if (counter.compareAndSet(x, x + 1)) {
                    break;
                }
            }
        }
    }

    @Actor
    public void compareAndSet2() {
        for (int i = 0; i < MAX; i++) {
            while (true) {
                int x = counter.get();
                if (counter.compareAndSet(x, x + 1)) {
                    break;
                }
            }
        }
    }

    @Actor
    public void compareAndSet3() {
        for (int i = 0; i < MAX; i++) {
            while (true) {
                int x = counter.get();
                if (counter.compareAndSet(x, x + 1)) {
                    break;
                }
            }
        }
    }

    @AfterActorsCompleted
    public void check() {
        Assert.assertEquals(3 * MAX, counter.get());
    }

}
