# Intro

This project aims to be a library to run tests, prooving that a concurrent algorithm or data structure behaves correctly (in regards to thread scheduling) or to find a thread scheduling order that breaks some invariant.

To do that, it injects 'checkpoints' into the code (according to user specifications) and runs the code with all thread scheduling combinations. With no other tests, it's at least able to identify possible starvation issues (a single thread is 'runnable' but is not able to make progress) or deadlocks caused by object monitor or java.util.concurrent.locks.Lock's or combination of those.

# Concepts

## Actor

## Checkpoints

## Execution Mode

# Configuration options

# What's working

As of this writing, not too much. It is able to detect the most basic forms of deadlocks and starvations, but thats about it.
With the help of the writer, it can also check for invariants at every point.

# Examples

# Planned Features

- Resume actors with action
  - for instance, spurious wakeup, exception thrown, etc
- 