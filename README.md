# Intro

This project aims to be a library to run tests, proving that a concurrent algorithm or data structure behaves correctly (in regards to thread scheduling) or to find a thread scheduling order that breaks some invariant.

To do that, it injects 'checkpoints' into the code (according to user specifications) and starts each thread, untill all of them reaches a Checkpoint. After that, it will select one of the threads to proceed until all the threads are completed.
If there was another thread that could be selected at that point, the test runner will later restart and select another thread, until all combinations have been exausted or an error occurs.

With no other checks in place, it is able to detect deadlocks happening due to blocking with monitors (synchronized) or java.util.concurrent.Locks, or possible infinite loops where only one thread can proceed at a time, but is unable to make progress.

In addition, you can add customized invariants that will be checked at each 'rendezvous' - at each step before a thread is selected to proceed - or a post condition to be checked when all threads finish.

Check the examples as they help clear up the air.

Its main inspiration was TLA+ and its model checker TLC.

# Concepts

## Scheduler
A scheduler is responsible for controlling the execution of the actors, making sure that at most one is running at a time.


## Test Scenario
A test scenrio is a class sort of like a JUnit test case class, except that it uses a special runner (ActorSchedulerRunner) and have one or more methods annotated as Actors (@Actor, @Actors or @MultipleActors). Each of those will be executed by a separate thread and the test runner will try to find all possible scheduling combinations.



## Run
A run is a scheduling combination until all of the actors terminate. Equivalent to a 'history' in this definition of [Link linearizability](https://en.wikipedia.org/wiki/Linearizability)

## Path
A path of a run is a description of a single run, in terms of what thread run at each point. Basically the description of the run.

## Execution Tree
In order to fully find all scheduling options, the runner mantains a tree where each node is a state that was reached and each 'link' represents selecting a particular actor to proceed leading to another node with the updated states.
The execution tree contain all of the explored runs or histories.

## Actor
An actor is basically a thread, managed by the scheduler. The actors are the basic working unit for the test, and you indicate them by the @Actor annotation. The scheduler will start the threads corresponding to each actor. (It is more clear looking at the examples).
As of now, the actor must be a public method with no arguments, but there are plans to have some things injected (notably, an ExecutorService or ScheduledExecutorService to start more actors with any type of task).

## Checkpoints
Actors run until they reach a checkpoint. The runtime waits for all actors to reach checkpoints and them choses one to proceed, based on availability (only threads that are not blocked by a monitor or Lock) and whether or not a 'path' has been fully explored.

## Parallel Execution
Because the runner acts like a scheduler allowing a single actor to proceed at any point, it will never use more than 1 cpu. In order to speedup the test, you can increase parallelism in the configuration. That way, the runner will run many schedulers in parallel.

## Execution Mode

### Renaming
(**Not recommended**) Renaming will create copies of the classes that will have checkpoints injected, but with an altered name.

### ClassLoaderIsolation
Works like renaming, except that the classes will have the same name as the original but they will be loaded by a fresh classloader for each parallel execution.
It should be the prefered mode, as it will provide better isolation, while being the fastest to start.

### Fork
(**currently not implemented**) Fork will spawn a fresh jvm instance for each scheduler. Forking allows injecting checkpoints in classes in the JRE

# Configuration options

TODO

# What's working

As of this writing, not too much. It is able to detect the most basic forms of deadlocks and starvations, but thats about it.
With the help of the writer, it can also check for invariants at every 'checkpoint'


#Getting Started

The best starting point would be to look at the simple SharedCounter example below:

```java
@RunWith(ActorSchedulerRunner.class)
public class SimpleSharedCounter {

    private volatile int counter;

    @Actor
    public void actor1() {
        counter++;
    }

    @Actor
    public void actor2() {
        counter++;
    }

    @Invariant
    public void invariant() {
        Assert.assertTrue(counter == 0 || counter == 1 || counter == 2);
    }

    @AfterActorsCompleted
    public void check() {
        Assert.assertEquals(2, counter);
    }

}
```

In this code, we have two actors that do the exact same thing: increment a shared (volatile) counter. 
It also contains an invariant (the counter cannot have a value that is not between 0 and 2) that will be checked at every step. For this particular example, the invariant will never be broken.
Finally, after all the threads finish, there's a post-condition check that verifies that no updates were missed. This post-condition will fail with some scheduling


# Examples

## Dining Philosophers
https://en.wikipedia.org/wiki/Dining_philosophers_problem

### DeadlockingPhilosophersTest
This test shows the naive implementation of the dining philosophers that will deadlock. 

### ResourceOrderedPhilosophersTest
This is one version of the Resource hierarchy solution, showing that total order of the locks being acquire prevent deadlocking the system as a whole.

### LockingPhilosophersTest
This version uses java.util.concurrent.Locks and makes the philosophers back down in case it can't pick up both forks. Intuitively this version should work, but this test shows that it can starve if all of the philosophers always end up picking up one of their forks, making all of them back down.
In practice, this version should work most of the time, but its demonstrably not starvation safe.

## Sleeping Barber
This test is currently not working, as conditional wait is not working properly. For instance, the following code:
```java
        synchronized (mutex) {
            while (!customerDone[index]) {
                mutex.wait();
            }
        }
```
will make this actor be able to be alwasy the one to be selected, but since the barber is never picked up, it will never proceed.
There needs to be a way to signal that a wait should be somewhat 'fair', in the sense that it should always yield. I don't want to simply disable spurious
wakeups (because well designed algorithms should take spurious wakeup into account), so the better option would be to limit the number of spurious wakeup that can happen.


## NonBlockingLinkedList
A very simple implementation of a non blockking linked list, as per Harris on [Link wiki](https://en.wikipedia.org/wiki/Non-blocking_linked_list).
This test shows that even a very simple implementation like this, in order to fully prove its correctness, the number of iterations necessary is gigantic - and consequently, the time to run the test is also very long.

# Planned Features

So many... For starter:
- wait / notify, condition.away, LockSupport.park, etc etc
- OffHeap tree - the exploration tree uses way too much memory. Ideally, it would be stored offheap (MappedByteBuffer) so swapping in and out of memory would be delegated to the OS
- Fork and ForkAll - there are scenarios that require forking to guarantee that everything gets cleaned up properly. But for fork mode, we need the OffHeap tree mapped to a file that can perform locks
- Support for other types of blocking resources (Thread.join, Future.get, Barrier, BlockingQueue, etc)
- UI to see the exploration graph. This will also require OffHeap tree
- Random exploration with maximum time. If this type of test is to be included in a regular build cycle (eg: mvn clean test), there has to be a way to make the scheduler run for a maximum period of time, exploring randomly.
- Auto detecting dependencies. Instead of specifying every class that should have checkpoints injected, the scheduler could / should try to detect which classes are touched and require checkpoint injection
- injection of ExecutorService / ScheduledExecutorService, maybe actor name and/or index
-

## Maybe Features
- Temporal properties (a la TLA+)


# Related libraries
Or as I like to say: if you're interested in this library, you may want to check these ones below, which are in a much more mature state:
- https://github.com/Kotlin/kotlinx-lincheck
    - a model checker + probabilistic checker written in kotlin. It can derive post-conditions automatically by comparing different execution scenarios
- https://github.com/openjdk/jcstress
    - a checker for concurrency issues. Its specially usefull for testing issues regarding the java memory model, which this library (and possibly lincheck above) cannot do by definition
- https://github.com/javapathfinder
    - a model checker for jvm programs written by NASA. It looks powerfull, but I couldn't make it work.

# Beware of exponential growth

Beware that the number of different runs and the time it takes for each one to complete grows exponential to the number of threads and checkpoints.
Consider how in the `NewThreadTest.java`, the number of runs and the time it takes to explore all runs increases very quickly:

| Number of threads | Different Runs | Execution time (seconds) |
|------------------:|---------------:|-------------------------:|
|                 1 |              3 |                      0.3 |
|                 2 |             45 |                      0.5 |
|                 3 |           1620 |                      1.5 |
|                 4 |         106920 |                      140 |