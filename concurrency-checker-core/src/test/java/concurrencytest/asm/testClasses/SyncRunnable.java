package concurrencytest.asm.testClasses;

public class SyncRunnable  implements Runnable {

    @Override
    public synchronized void run() {
        this.notifyAll();
    }
}
