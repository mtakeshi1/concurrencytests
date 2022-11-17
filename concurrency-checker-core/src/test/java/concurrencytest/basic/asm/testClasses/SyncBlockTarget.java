package concurrencytest.basic.asm.testClasses;

public class SyncBlockTarget implements Runnable {


    @Override
    public void run() {
        synchronized (this) {
            System.out.println("bla");
        }
    }
}
