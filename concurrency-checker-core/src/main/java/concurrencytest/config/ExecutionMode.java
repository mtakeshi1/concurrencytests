package concurrencytest.config;

/**
 * Execution mode controls how the checkpoints will be injected and the test will run.
 */
public enum ExecutionMode {

    /**
     * Defaults to {@link ExecutionMode#CLASSLOADER_ISOLATION} if no classes from the JRE are in the classes to be enhanced list
     */
    AUTO,
    /**
     * Creates copy of classes with the checkpoints injected, but delegate the classloading to standard classpath mechanism.
     * The forked java process will run all of the parallel executions
     * Can be used to inject checkpoints in JRE classes.
     */
    FORK,

    /**
     * Behave the same as {@link ExecutionMode#FORK} except that each parallel execution will fork another java process.
     */
    FORK_ALL,

    /**
     * Creates copy of classes to be enhanced with different names, loads them in a {@link concurrencytest.asm.utils.OpenClassLoader}
     * and the tests are run in the same JVM
     */
    RENAMING,

    /**
     * Creates copy of classes with the checkpoints injected with the same names, but uses one {@link concurrencytest.asm.utils.SpecialClassLoader}
     * per test thread to isolate classes. The tests are within within same JVM.
     */
    CLASSLOADER_ISOLATION


}
