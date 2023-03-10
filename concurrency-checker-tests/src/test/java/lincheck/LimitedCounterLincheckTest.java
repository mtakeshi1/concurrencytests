package lincheck;

import counter.LimitedCounter;
import org.jetbrains.kotlinx.lincheck.LinCheckerKt;
import org.jetbrains.kotlinx.lincheck.annotations.Operation;
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions;
import org.junit.Test;

public class LimitedCounterLincheckTest {

    private LimitedCounter limitedCounter = new LimitedCounter(2);

    @Operation
    public int increment() {
        return limitedCounter.inc();
    }

    @Test
    public void test() {
        ModelCheckingOptions mcOptions = new ModelCheckingOptions();
//        StressOptions options = new StressOptions();
        LinCheckerKt.check(mcOptions, LimitedCounterLincheckTest.class);
    }


}
