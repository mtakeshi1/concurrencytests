package concurrencytest.runtime.exception;

/**
 * Signals that a whole scheduler should be aborted. Probably the initial path selected is blocked or was otherwise completed
 */
public class SchedulerAbortedException extends RuntimeException {
}
