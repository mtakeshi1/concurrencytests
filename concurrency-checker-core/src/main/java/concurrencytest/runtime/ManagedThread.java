package concurrencytest.runtime;

public class ManagedThread extends Thread {
    public ManagedThread() {
    }

    public ManagedThread(Runnable target) {
        super(target);
    }

    public ManagedThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public ManagedThread(String name) {
        super(name);
    }

    public ManagedThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public ManagedThread(Runnable target, String name) {
        super(target, name);
    }

    public ManagedThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public ManagedThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    public ManagedThread(ThreadGroup group, Runnable target, String name, long stackSize, boolean inheritThreadLocals) {
        super(group, target, name, stackSize, inheritThreadLocals);
    }

    @Override
    public void run() {
        super.run();
    }
}
