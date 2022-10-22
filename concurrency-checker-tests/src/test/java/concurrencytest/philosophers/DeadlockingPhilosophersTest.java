package concurrencytest.philosophers;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(ActorSchedulerRunner.class)
public class DeadlockingPhilosophersTest {

    private static final int NUM = 3;

    private Spoon[] spoons;

    private boolean[] eaten;

    @Before
    public void setup() {
        spoons = new Spoon[NUM];
        eaten = new boolean[NUM];
        for (int i = 0; i < NUM; i++) {
            spoons[i] = new Spoon();
        }
    }

    @Actor
    public void philo0() {
        philosopher(0);
    }

    @Actor
    public void philo2() {
        philosopher(2);
    }

    @Actor
    public void philo1() {
        philosopher(1);
    }

    public void philosopher(int index) {
        Spoon left = spoons[index];
        Spoon right = spoons[(index + 1) % spoons.length];
        synchronized (left) {
            left.pickup();
            synchronized (right) {
                right.pickup();
                eaten[index] = true;
                right.putDown();
            }
            left.putDown();
        }
    }

    @Invariant
    public void spoonsShouldBeUsedByOne() {
        for (Spoon spoon : spoons) {
            Assert.assertTrue(spoon.getUsed() <= 1);
        }
    }

    @AfterActorsCompleted
    public void allEaten() {
        for (boolean satisfied : eaten) {
            Assert.assertTrue(satisfied);
        }
    }

}
