package sut;

import concurrencytest.annotations.Actor;
import org.junit.After;
import org.junit.Assert;

public class GenerateCheckpointsTest {

    private volatile int one;
    private volatile long two;

    public void checkParameterOrder(int x, long xx, char c, double d) {
        this.one = x;
        this.two = xx;
    }

    @After
    public void after() {
        Assert.assertEquals(1, one);
        Assert.assertEquals(2L, two);
    }

    @Actor
    public void actor1() {
        checkParameterOrder(1, 2L, 'a', 10.0);
    }

    @Actor
    public void actor2() {
        checkParameterOrder(1, 2L, 'b', 20.0);
    }

}
