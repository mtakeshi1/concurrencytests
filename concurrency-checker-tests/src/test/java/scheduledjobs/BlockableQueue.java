package scheduledjobs;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public record BlockableQueue(Queue<String> queue, AtomicBoolean blocked) {

    public BlockableQueue() {
        this(new LinkedBlockingQueue<>(), new AtomicBoolean());
    }

    public boolean add(String value) {
        if (blocked.get()) {
            return false;
        }
        synchronized (this) {
            if (blocked.get()) {
                return false;
            }
            queue.add(value);
            return true;
        }
    }

    public synchronized boolean block() {
        return blocked.compareAndSet(false, true);
    }


}
