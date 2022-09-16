package concurrencytest.asm.testClasses;

import concurrencytest.runtime.CheckpointRuntimeAccessor;
import concurrencytest.annotations.FieldCheckpoint;
import concurrencytest.annotations.InjectionPoint;

@FieldCheckpoint(fieldNameRegex = ".+", injectionPoints = {InjectionPoint.BEFORE, InjectionPoint.AFTER})
public class InjectionTarget implements Runnable {

    public Integer intPublicField;
    private float floatPrivateField;

    @Override
    public void run() {
        System.out.println(intPublicField);
        for(int i = 0; i < 10; i++) {
            CheckpointRuntimeAccessor.manualCheckpoint();
        }
    }
}
