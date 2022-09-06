package concurrencytest.util;

public class ArrayUtils {
    public static <E> boolean arrayContains(E[] accessModifiers, E modifier) {
        for (var m : accessModifiers) {
            if (m.equals(modifier)) {
                return true;
            }
        }
        return false;
    }
}
