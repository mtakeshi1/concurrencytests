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

}
