package concurrencytest.basic.asm.testClasses;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockTest implements Runnable {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Override
    public void run() {
        doWithLock(() -> System.out.println("locked"));
    }

    public void doWithLock(Runnable action) {
        readWriteLock.readLock().lock();
        action.run();
        readWriteLock.readLock().unlock();
    }

}
