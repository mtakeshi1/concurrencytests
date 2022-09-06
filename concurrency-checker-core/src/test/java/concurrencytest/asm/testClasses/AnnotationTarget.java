package concurrencytest.asm.testClasses;

import concurrencytest.annotations.FieldCheckpoint;
import concurrencytest.annotations.InjectionPoint;

@FieldCheckpoint(fieldNameRegex = ".+", injectionPoints = {InjectionPoint.BEFORE, InjectionPoint.AFTER})
public class AnnotationTarget implements Runnable {

    public Integer intPublicField;
    private float floatPrivateField;

    @Override
    public void run() {
        System.out.println(intPublicField);
    }
}
