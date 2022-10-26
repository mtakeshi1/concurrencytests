package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicInteger;

public class NewThreadTest {

    public static final int THREAD_COUNT = 4;

    private final AtomicInteger counter = new AtomicInteger();

    public void increment() {
        counter.incrementAndGet();
    }

    @Actor
    public void main() {
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread t = new Thread(this::increment);
            t.start();
        }
    }

    @AfterActorsCompleted
    public void count() {
        Assert.assertEquals(THREAD_COUNT, counter.get());
    }

}
