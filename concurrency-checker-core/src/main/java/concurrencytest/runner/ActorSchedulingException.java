package concurrencytest.runner;

public abstract class ActorSchedulingException extends Exception {
    public ActorSchedulingException(String message) {
        super(message);
    }

    public ActorSchedulingException() {
    }
}
