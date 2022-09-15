package concurrencytest.asm;

import org.objectweb.asm.Type;

import java.lang.invoke.VarHandle;

public class SpecialMethods {

    public static boolean isSpecialVolatileAccess(Type target, String methodName, String methodDescriptor) {
        if (target.getClassName().equals("sun.mist.Unsafe")) {
            return true;
        }
        if (target.getClassName().equals(VarHandle.class.getName())) {
            return true;
        }
        return false;

    }

}
