package concurrencytest.asm.testClasses;

import java.util.concurrent.Callable;

public class SyncCallable implements Callable<Integer> {

    @Override
    public synchronized Integer call() {
        if (System.currentTimeMillis() > 0) {
            return Integer.MAX_VALUE;
        } else {
            return Integer.MIN_VALUE;
        }
    }


}
