# Currently working on:
- Clean-up of various FIXME and TODO
- Documentation
- Examples
- callbacks should be invoked by the scheduler thread and not by the actor threads
- bytebuffer backed trees 
  - direct  byte buffer trees allow smaller heap, with the OS managing swapping memory in / out of disk
  - mapped (files) bytebuffer trees allow forking 
- injection of ExecutorService / ScheduledExecutorService into @Before and actors
- better test infrastructure - mostly to detect which classes to inject for the basic tests
- writing README.md
- run tests against mutable runtime state
  - it seems that mutable runtime state callbacks can be called from multiple threads?

# nice to haves
- auto detect classes to inject checkpoints
- unify causes of blocking
- write a non-mutable runtimestate. That will require many changes to how checkpoints are listened to
- StackTrackingVisitor
  - should also track constant LDC's
- fork mode should take special notice of the  InitialPathBlockedException as the initial scheduler is blind and will assign possibly blocked paths to forks

# planned
- add checkpoint for exception catching
- wait and notify - actually are they necessary?
- park / unpark
- Fork to another jvm
  - we could export the classes to the other jvm via standard classpath mechanism
  - jdk classes must be patched https://openjdk.org/jeps/261
  - https://learn.microsoft.com/en-us/java/openjdk/transition-from-java-8-to-java-11
  - https://openjdk.org/projects/jigsaw/quick-start#xoverride
