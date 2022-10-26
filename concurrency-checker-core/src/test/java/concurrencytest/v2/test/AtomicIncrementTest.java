package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIncrementTest {

    private final AtomicInteger count = new AtomicInteger();

    @Actor
    public void actor1() {
        count.incrementAndGet();
    }

    @Actor
    public void actor2() {
        count.incrementAndGet();
    }

    @Actor
    public void actor3() {
        count.incrementAndGet();
    }

//    @Actor
    public void actor4() {
        count.incrementAndGet();
    }

//    @Actor
    public void actor5() {
        count.incrementAndGet();
    }

    @AfterActorsCompleted
    public void sanity() {
//        Assert.assertEquals(3, count.get());
    }


}
