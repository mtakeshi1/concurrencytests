# Currently working on:
- Locks 
  - more tests
- BlockCause should carry the information about the resource so it knows if is blocked by anything other than locks / monitor. That way, we can let the scheduler know if a thread can proceed or not (for instance, if the BlockCause is THREAD_JOIN and it carries the thread that is being joined in, it can check for Thread.isAlive)
- add checkpoint for exception catching
- callbacks should be invoked by the scheduler thread and not by the actor threads
- Unsafe?
- off-heap graph - allows forking
- Fork to another jvm
  - we could export the classes to the other jvm via standard classpath mechanism
  - jdk classes must be patched https://openjdk.org/jeps/261
  - https://learn.microsoft.com/en-us/java/openjdk/transition-from-java-8-to-java-11
  - https://openjdk.org/projects/jigsaw/quick-start#xoverride
- injection of ExecutorService / ScheduledExecutorService into @Before and actors
- better test infrastructure - mostly to detect which classes to inject for the basic tests
- writing README.md
- fork mode should take special notice of the  InitialPathBlockedException as the initial scheduler is blind and will assign possibly blocked paths to forks
- run tests against mutable runtime state
  - it seems that mutable runtime state callbacks can be called from multiple threads?

# nice to haves
- auto detect classes to inject checkpoints
- unify causes of blocking
- write a non-mutable runtimestate. That will require many changes to how checkpoints are listened to
- StackTrackingVisitor
  - should also track constant LDC's
# planned
- wait and notify
- park / unpark

 