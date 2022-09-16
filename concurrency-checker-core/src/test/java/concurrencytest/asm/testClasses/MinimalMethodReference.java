package concurrencytest.asm.testClasses;

public class MinimalMethodReference {

    public void bla() {
        Runnable run = System.out::println;
        run.run();
    }

    public static void main(String[] args) {
        //"java.lang.invoke.MethodHandle.TRACE_METHOD_LINKAGE"
        System.setProperty("java.lang.invoke.MethodHandle.TRACE_METHOD_LINKAGE", "true");
        new MinimalMethodReference().bla();
    }

}
