package sut;

import concurrencytest.annotations.Actor;

import java.util.NoSuchElementException;

public class ActorError {

    @Actor
    public void mistake() {
        throw new NoSuchElementException();
    }

}
