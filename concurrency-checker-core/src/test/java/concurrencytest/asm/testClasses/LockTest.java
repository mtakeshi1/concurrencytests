package concurrencytest.asm.testClasses;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockTest implements Runnable {

    private final Lock lock = new ReentrantLock();


    @Override
    public void run() {
        try {
            if (lock.tryLock(1, TimeUnit.MINUTES)) {
                System.out.println("acquired");
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
