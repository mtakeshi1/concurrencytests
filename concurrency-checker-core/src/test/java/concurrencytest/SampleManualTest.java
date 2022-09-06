package concurrencytest;


import concurrencytest.annotations.Actor;
import concurrencytest.annotations.InstrumentationStrategy;
import concurrencytest.annotations.TestParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(ConcurrencyRunner.class)
@TestParameters(instrumentationStrategy = InstrumentationStrategy.NONE)
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
        TestRuntimeImpl.autoCheckpoint(this);
        int localCount = counter + 1;
        TestRuntimeImpl.autoCheckpoint(this);
        counter = localCount;
        testArray(null, null, null, null, null, null);
    }

    public void testArray(int[] onedim, int[][] twodim, String[] strings, long[] a, short[] c, char[][][] chars) {

    }

    @Actor
    public void actor2() {
        TestRuntimeImpl.autoCheckpoint(this);
        int localCount = counter + 2;
        TestRuntimeImpl.autoCheckpoint(this);
        counter = localCount;
    }

    @After
    public void checkAssertions() {
//        Assert.assertEquals(3, counter);
    }
}
