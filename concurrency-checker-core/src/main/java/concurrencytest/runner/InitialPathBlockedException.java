package concurrencytest.runner;

public class InitialPathBlockedException extends ActorSchedulingException {
    public InitialPathBlockedException(String cause) {
        super(cause);
    }
}
