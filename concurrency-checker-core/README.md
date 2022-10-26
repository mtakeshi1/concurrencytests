# Intro

This project aims to be a library to run tests, prooving that a concurrent algorithm or data structure behaves correctly (in regards to thread scheduling) or to find a thread scheduling order that breaks some invariant.

To do that, it injects 'checkpoints' into the code (according to user specifications) and runs the code with all thread scheduling combinations. With no other tests, it's at least able to identify possible starvation issues (a single thread is 'runnable' but is not able to make progress) or deadlocks caused by object monitor or java.util.concurrent.locks.Lock's or combination of those.

# Concepts

## Test Scenario


## Run

## Path

## Execution Graph

## Actor
An actor is basically a thread, managed by the scheduler. The actors are the basic working unit for the test, and you indicate them by the @Actor annotation. The scheduler will start the threads corresponding to each actor. (It is more clear looking at the examples).
As of now, the actor must be a public method with no arguments, but there are plans to have some things injected (notably, an ExecutorService or ScheduledExecutorService to start more actors with any type of task).

## Checkpoints
Actors run until they reach a checkpoint. The runtime waits for all actors to reach checkpoints and them choses on to proceed, based on availability (only threads that are not blocked by a monitor or Lock) and whether or not a 'path' has been fully explored.


## Execution Mode

Currently, only the Renaming executiom mode is supported. That means that classes that are subjected to Checkpoint injection have their bytecode copied to another class with a suffix generated randomly.


# Configuration options

# What's working

As of this writing, not too much. It is able to detect the most basic forms of deadlocks and starvations, but thats about it.
With the help of the writer, it can also check for invariants at every point.

# Examples

# Planned Features

# Beware of exponential growth

Beware that the number of different runs and the time it takes for each one to complete grows exponential to the number of threads and checkpoints.
Consider how in the `NewThreadTest.java`, the number of runs and the time it takes to explore all runs increases very quickly:

| Number of threads | Different Runs | Execution time (seconds) |
|------------------:|---------------:|-------------------------:|
|                 1 |              3 |                      0.3 |
|                 2 |             45 |                      0.5 |
|                 3 |           1620 |                      1.5 |
|                 4 |         106920 |                      140 |