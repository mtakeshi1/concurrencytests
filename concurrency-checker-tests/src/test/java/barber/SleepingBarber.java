package barber;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.MultipleActors;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.annotations.v2.ConfigurationSource;
import concurrencytest.asm.ArrayElementMatcher;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

/**
 * Sleeping barber solution, as per
 * <p>
 * https://en.wikipedia.org/wiki/Sleeping_barber_problem
 */
@RunWith(ActorSchedulerRunner.class)
public class SleepingBarber {

    public static final int CUSTOMERS = 2;

    private boolean[] customerDone;

    private boolean[] customerWaiting;

    private Object[] customerMutex;

    private final Object roomMutex = new Object();

    @Before
    public void setup() {
        customerDone = new boolean[CUSTOMERS];
        customerWaiting = new boolean[CUSTOMERS];
        customerMutex = new Object[CUSTOMERS];
        for (int i = 0; i < CUSTOMERS; i++) {
            customerMutex[i] = new Object();
        }
    }

    @Actor
    public void barber() throws InterruptedException {
        while (!finished()) {
            System.out.println("waiting for customer");
            waitForCustomer();
            for (int i = 0; i < CUSTOMERS; i++) {
                if (customerWaiting[i]) {
                    System.out.println("cutting hair for customer " + i);
                    cutHair(i);
                    break;
                }
            }
        }
    }

    @MultipleActors(numberOfActors = CUSTOMERS)
    public void customer(int index) throws InterruptedException {
        System.out.println("before room mutex " + index);
        synchronized (roomMutex) {
            customerWaiting[index] = true;
            roomMutex.notifyAll();
        }

        Object mutex = this.customerMutex[index];
        synchronized (mutex) {
            while (!customerDone[index]) {
                mutex.wait();
            }
        }
    }

    @AfterActorsCompleted
    public void allHairsCut() {
        for (int i = 0; i < CUSTOMERS; i++) {
            Assert.assertTrue("customer " + i + " not cut", customerDone[i]);
        }
    }

    @AfterActorsCompleted
    public void noWaitingCustomer() {
        for (int i = 0; i < CUSTOMERS; i++) {
            Assert.assertFalse("customer " + i + " still waiting", customerWaiting[i]);
        }
    }

    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(SleepingBarber.class) {
            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }

            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public Collection<ArrayElementMatcher> arrayCheckpoints() {
                        return List.of();
                    }
                };
            }

            @Override
            public int maxLoopIterations() {
                return 2;
            }
        };
    }

    private void cutHair(int index) {
        Object mutex = this.customerMutex[index];
        synchronized (mutex) {
            customerDone[index] = true;
            mutex.notifyAll();
            System.out.printf("Hair cut %d%n", index);
        }
        synchronized (roomMutex) {
            customerWaiting[index] = false;
        }
    }

    private void waitForCustomer() throws InterruptedException {
        synchronized (roomMutex) {
            while (!hasWaitingCustomer()) {
                roomMutex.wait();
            }
        }
    }

    private boolean hasWaitingCustomer() {
        for (boolean c : customerWaiting) {
            if (c) return true;
        }
        return false;
    }

    private boolean finished() {
        for (boolean c : customerDone) {
            if (!c) return false;
        }
        return true;
    }


}
