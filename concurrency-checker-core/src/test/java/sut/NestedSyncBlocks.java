package sut;

import concurrencytest.annotations.Actor;
import org.junit.Before;
import sut.mock.Session;
import sut.mock.SessionManager;
import sut.mock.SessionState;

public class NestedSyncBlocks {

    private final SessionManager sessionManager = new SessionManager();

    private final Session sharedSession = new Session();

    private final SessionState state = new SessionState("brand new session");

    @Before
    public void setup() {
    }

    @Actor
    public void connect() {
        sessionManager.start();
    }

    @Actor
    public void failure() {
        sessionManager.stop();
    }
}
