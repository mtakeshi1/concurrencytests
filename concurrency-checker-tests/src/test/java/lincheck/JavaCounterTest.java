package lincheck;

import kotlin.jvm.Volatile;
import org.jetbrains.kotlinx.lincheck.LinCheckerKt;
import org.jetbrains.kotlinx.lincheck.annotations.Operation;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions;
import org.junit.Test;

public class JavaCounterTest {

    public static class Counter {
        @Volatile
        private int count = 0;

        public void inc() {
            count++;
        }

        public int get() {
            return count;
        }
    }

    private Counter counter = new Counter();

    @Operation
    public void increment() {counter.inc();}

    @Operation
    public int get() {
        return counter.get();
    }

    @Test
    public void test() {
        StressOptions options = new StressOptions();
        LinCheckerKt.check(options, JavaCounterTest.class);
//        LinCheckerKt.check(JavaCounterTest.class, new StressOptions());
    }
}
