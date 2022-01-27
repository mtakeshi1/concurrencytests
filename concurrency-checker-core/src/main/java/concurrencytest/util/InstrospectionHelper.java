package concurrencytest.util;

public class InstrospectionHelper {
    public static String findRelevantStackInfo() {
        Throwable t = new Throwable().fillInStackTrace();
        for (var stack : t.getStackTrace()) {
            if (!stack.getClassName().startsWith("concurrencytest")) {
                return stack.toString();
            }
        }
        return null;
    }
}
