# Currently working on:
- Locks and other interesting checkpoints
  - java.util.concurrent.atomic.*
- callbacks should be invoked by the scheduler thread and not by the actor threads
- Unsafe?
- off-heap graph - allows forking
- Fork to another jvm
  - we could export the classes to the other jvm via standard classpath mechanism
  - jdk classes must be patched https://openjdk.org/jeps/261
  - https://learn.microsoft.com/en-us/java/openjdk/transition-from-java-8-to-java-11
  - https://openjdk.org/projects/jigsaw/quick-start#xoverride
- making @After call (in case of failure)
- test actors spawning threads
- injection of ExecutorService / ScheduledExecutorService into @Before and actors
- better test infrastructure - mostly to detect which classes to inject for the basic tests
- writing README.md
 
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