package concurrencytest;

public interface RenamingInterface {

    default RenamingInterface returnThisInterface() {
        return this;
    }


}
