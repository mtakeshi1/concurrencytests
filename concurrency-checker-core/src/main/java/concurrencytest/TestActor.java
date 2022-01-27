package concurrencytest;

public class TestActor {
    private final String actorName;
    private final Runnable task;

    public TestActor(String name, Runnable task) {
        this.actorName = name;
        this.task = task;
    }

    public String getName() {
        return actorName;
    }

    public Runnable getTask() {
        return task;
    }
}
