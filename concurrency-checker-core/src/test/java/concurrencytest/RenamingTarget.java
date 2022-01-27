package concurrencytest;

public class RenamingTarget implements RenamingInterface {

    public RenamingTarget targetField = this;

    public RenamingInterface interfaceField = this;

    public RenamingTarget returnThis() {
        return this;
    }

    public RenamingTarget[] returnArray() {
        return new RenamingTarget[]{this};
    }

    public class RenamingInnerClass {

    }

    public static class RenamingStaticClass implements RenamedInnerInterface {

    }

    public interface RenamedInnerInterface {

    }

    public RenamingInnerClass innerClass() {
        return new RenamingInnerClass();
    }

    public RenamingStaticClass innerStaticClass() {
        return new RenamingStaticClass();
    }



    public RenamingTarget[][] returnArray2d() {
        return new RenamingTarget[1][1];
    }

    public boolean exceptionCaught() {
        try {
            throw new RenamedException();
        } catch (RenamedException e) {
            return true;
        }
    }

    public boolean exceptionThrown() throws Exception {
        throw new RenamedException();
    }

    public int[] returnIntArray() {
        return null;
    }

    public int[][][] returnIntArray3() {
        return null;
    }

    public boolean selfCheck() {
        return returnArray() != null &&
                returnArray()[0] instanceof RenamingTarget &&
                returnArray()[0] instanceof RenamingInterface &&
                targetField instanceof RenamingTarget &&
                targetField instanceof RenamingInterface &&
                interfaceField instanceof RenamingTarget &&
                interfaceField instanceof RenamingInterface;
    }

}
