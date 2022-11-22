package concurrencytest.runtime.exception;

/**
 * Signals that the current run should be aborted due to not finding any unexplored path
 */
public class RunAbortedException extends RuntimeException {
}
