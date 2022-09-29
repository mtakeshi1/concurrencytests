package concurrencytest.runner;

public class MaxLoopCountViolationException extends ActorSchedulingException {
    public MaxLoopCountViolationException(String actor, int maxLoopCount) {
    }
}
