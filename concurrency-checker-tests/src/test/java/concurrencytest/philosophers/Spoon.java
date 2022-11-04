package concurrencytest.philosophers;

public class Spoon {

    private volatile int used;

    public void pickup() {
        used++;
    }

    public void putDown() {
        used--;
    }

    public int getUsed() {
        return used;
    }
}
