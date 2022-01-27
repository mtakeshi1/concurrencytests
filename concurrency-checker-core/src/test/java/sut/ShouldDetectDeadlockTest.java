package sut;

import concurrencytest.annotations.Actor;

public class ShouldDetectDeadlockTest {

    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    public static volatile int runCount = 0;

    @Actor
    public void actor1() {
        synchronized (lock1) {
            synchronized (lock2) {
                runCount++;
            }
        }
    }

    @Actor
    public void actor2() {
        synchronized (lock2) {
            synchronized (lock1) {
                runCount++;
            }
        }
    }
}
