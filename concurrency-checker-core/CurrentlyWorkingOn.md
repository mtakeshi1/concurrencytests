# Currently working on:
- Removing old code - mostly completed
- better test infrastructure - mostly to detect which classes to inject for the basic tests
- off-heap graph - allows forking
- Fork to another jvm
  - we could export the classes to the other jvm via standard classpath mechanism
- java.util.concurrent specific classes and methods
  - easy ones are atomicX and lock
- making @After call (in case of failure)
- test actors spawning threads
- injection of ExecutorService / ScheduledExecutorService into @Before and actors
- tests witj j.u.c.l.Locks
- writing README.md
- callbacks should be invoked by the scheduler thread and not by the actor threads. 
- run tests against mutable runtime state

# nice to haves
- auto detect classes to inject checkpoints
- unify causes of blocking
- write a non-mutable runtimestate. That will require many changes to how checkpoints are listened to

# planned / done checkpoint types
- field access - missing pushing field details to stack
- missing inspecting stack to fill in monitor type
- wait and notify
- checkpoint location shouldn't be ambiguous

# open questions
- should we persist / serialize thread state / details with the checkpoint?
- should we special case java.util.concurrent.Lock?
  - how about other concurrency 'primitives'?
- how to deal with conditional Lock.tryLock
- 