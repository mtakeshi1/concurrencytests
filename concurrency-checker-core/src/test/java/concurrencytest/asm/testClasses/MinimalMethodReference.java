package concurrencytest.asm.testClasses;

public class MinimalMethodReference {

    public void bla() {
        Runnable run = System.out::println;
        run.run();
    }

}
