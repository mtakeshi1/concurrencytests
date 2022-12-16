package concurrencytest.basic.asm.testClasses;

public class WaitNotifyTestTarget implements Runnable {

    private final Object lock = new Object[0];


    @Override
    public void run() {
        try {
            synchronized (lock) {
                lock.notify();
                lock.notifyAll();
                lock.wait(100);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
