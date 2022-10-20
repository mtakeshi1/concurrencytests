package concurrencytest.runner;

public class DeadlockFoundException extends ActorSchedulingException {
    public DeadlockFoundException(String cause) {
        super(cause);
    }
}
