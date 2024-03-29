package sut;

public class SynchronizedValueHolder {
    private volatile int value;

    public synchronized int getValue() {
        return value;
    }

    public synchronized void increment() {
        value++;
    }

    public synchronized void setValue(int value) {
        this.value = value;
    }
}
