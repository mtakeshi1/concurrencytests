package concurrencytest;


import concurrencytest.annotations.Actor;
import concurrencytest.runner.ActorSchedulerRunner;
import concurrencytest.runtime.CheckpointRuntimeAccessor;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(ActorSchedulerRunner.class)
public class SampleManualTest {

    private volatile int counter;

    @Before
    public void setup() {
        counter = 0;
    }

    public synchronized void syncMethod() {
        counter++;
    }


    @Actor
    public void actor1() {
        CheckpointRuntimeAccessor.manualCheckpoint();
        int localCount = counter + 1;
        CheckpointRuntimeAccessor.manualCheckpoint();
        counter = localCount;
        testArray(null, null, null, null, null, null);
    }

    public void testArray(int[] onedim, int[][] twodim, String[] strings, long[] a, short[] c, char[][][] chars) {

    }

    @Actor
    public void actor2() {
        CheckpointRuntimeAccessor.manualCheckpoint();
        int localCount = counter + 2;
        CheckpointRuntimeAccessor.manualCheckpoint();
        counter = localCount;
    }

    @After
    public void checkAssertions() {
//        Assert.assertEquals(3, counter);
    }
}
