package concurrencytest.util;

public class Utils {

    private Utils() {
    }


    public static <E> E todo() {
        return todo("");
    }

    public static <E> E todo(String message) {
        throw new RuntimeException("Not yet implemented: %%s%s".formatted(message));
    }

    public static boolean notEmpty(String details) {
        return details != null && details.length() > 0;
    }
}
