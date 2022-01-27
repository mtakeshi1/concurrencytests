package sut;

public class StaticSynchronizedValueHolder {

    private static int value;

    public static synchronized int getValue() {
        return value;
    }

    public static synchronized void increment() {
        value++;
    }

    public static void reset() {
        value = 0;
    }
}
