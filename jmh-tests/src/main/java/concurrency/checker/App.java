package concurrency.checker;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws Exception {
        final Object a = new Object();
        final Object b = new Object();
        final Object c = new Object();
        final AtomicBoolean atomicBoolean = new AtomicBoolean();
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (a) {
                        synchronized (b) {
                            synchronized (c) {
                                atomicBoolean.set(true);
                                c.wait();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
        thread.join();
        System.out.println("Hello World!");
    }
}
