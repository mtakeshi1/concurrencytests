package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;

//@RunWith(ActorSchedulerRunner.class)
public class SimpleSharedCounter {

    private volatile int counter;

    @Actor
    public void actor1() {
        counter++;
    }

    @Actor
    public void actor2() {
        counter++;
    }

    @Actor
    public void actor3() {
        counter++;
    }
//    @Invariant
//    public void invariant() {
//        Assert.assertTrue(counter == 0 || counter == 1 || counter == 2);
//    }
//
//    @AfterActorsCompleted
//    public void check() {
//        Assert.assertEquals(2, counter);
//    }

}
