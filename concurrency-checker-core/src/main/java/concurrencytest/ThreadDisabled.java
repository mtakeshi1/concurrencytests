package concurrencytest;

public class ThreadDisabled extends Error {

    public static ThreadDisabled INSTANCE = new ThreadDisabled();

    private ThreadDisabled() {
        super("testing runtime was disabled");
    }
//
//    @Override
//    public synchronized Throwable fillInStackTrace() {
//        return this;
//    }
}
