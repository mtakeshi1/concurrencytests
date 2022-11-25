package sut;

import concurrencytest.annotations.MultipleActors;
import concurrencytest.annotations.AfterActorsCompleted;
import org.junit.Assert;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockUnlock {

    public static final int NUM_ACTORS = 2;

    private final Lock left = new ReentrantLock();
    private final Lock right = new ReentrantLock();

    private int count;

    @MultipleActors(numberOfActors = NUM_ACTORS)
    public void actors() {
        left.lock();
        try {
            right.lock();
            count++;
            right.unlock();
        } finally {
            left.unlock();
        }
    }

    @AfterActorsCompleted
    public void checkCount() {
        Assert.assertEquals(NUM_ACTORS, count);
    }

}
