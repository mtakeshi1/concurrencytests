package concurrencytest.asm.testClasses;

import java.util.function.LongSupplier;

public class MethodReferenceTest {

    public void bla() {
        LongSupplier run = System::currentTimeMillis;
        run.getAsLong();
    }

}
