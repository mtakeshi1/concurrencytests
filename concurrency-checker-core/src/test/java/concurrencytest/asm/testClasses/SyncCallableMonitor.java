package concurrencytest.asm.testClasses;

import java.util.concurrent.Callable;

public class SyncCallableMonitor implements Callable<Void> {
    @Override
    public synchronized Void call() throws Exception {
        this.notify();
        return null;
    }
}
