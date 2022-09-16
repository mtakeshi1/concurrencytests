package concurrencytest.asm.testClasses;

public class MinimalLambda {

    public void bla() {
        Runnable run = () -> System.out.println("yey");
        run.run();
    }

}
