package concurrencytest.asm.testClasses;

import concurrencytest.CheckpointRuntimeAccessor;

public class InjectionTarget2 implements Runnable{

    public String label = "a";

    @Override
    public void run() {
        CheckpointRuntimeAccessor.manualCheckpoint(label);
    }
}
