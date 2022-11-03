package concurrencytest.checkpoint.description;

import java.util.concurrent.locks.Lock;

public record TryAcquireWithResult(Lock lock, boolean acquired) {

    public static TryAcquireWithResult from(boolean result, Lock lock) {
        return new TryAcquireWithResult(lock, result);
    }

    public static TryAcquireWithResult from(Lock lock) {
        return new TryAcquireWithResult(lock, true);
    }

}
