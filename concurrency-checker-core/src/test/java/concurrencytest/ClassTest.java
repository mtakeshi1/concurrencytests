package concurrencytest;

public class ClassTest {

    private final Object something = new Object();

    public static synchronized Object syncStaticMethod(long a, int x, Object aa) {
        return null;
    }

    public void checkSimpleSynchronized() {
        synchronized (something) {
            System.out.printf("a%d%d", 1, 2);
        }
    }

    public Object bla() throws Throwable {
        Object maybeMonitor = ClassTest.class;
        boolean is = TestRuntime.checkActualDispatchForStaticMethod((Class<?>) maybeMonitor, "syncStaticMethod", "", 1, "", "");
        try {
            Object o = syncStaticMethod(1, 1, null);
            return o;
        } finally {
            if (is) {
                TestRuntime.afterMonitorReleasedCheckpoint(maybeMonitor, 2, "", "");
            } else {
                TestRuntime.checkpointReached(3, "", "");
            }
        }
    }

}
