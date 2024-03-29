Roadmap of features from a high level perspective


# Before setting up CI
- More examples
- Documentation
- Fix bugs
  - notably wait / notify not working as intended

# Before I consider 'usable':
- wait / notify
- ConfigurationBuilder
    - configurable checkpoint injector per injected class
    - per method / checkpoint timeout?
- Merge CheckpointReached with Checkpoint
    - improving usage of 'context' to actually carry runtime context into CheckpointReached
    - better toString
- Random execution with max time
    - random exploration for when tests are taking too long
    - the new scheduler makes this a lot easier
- wait / notify / lock.condition coordination
- OffHeapTree
- random / sleep detection
    - random should be seeded or bypassed
- more blocking resources:
    - blocking queue
    - Thread.join
    - Condition.await
    - CyclicBarrier
    - Semaphore
    - LockSupport.park

# For the future
- repeated state count (if the system as a whole is not making progress)
    - hash over threadstates to try to detect a 'loop'
- Injection on java rt classes
- Resume actors with action
    - for instance, spurious wakeup, exception thrown, etc
- Recorded output and re-run with recorded output
- Better throughput (better CPU usage)
    - maybe we can swap the semaphore for LockSupport.park / unpark?
- Data race analysis (look at the bytecode, see shared objects and generate checkpoints based on those)
- better support for loops (eg: use the loopcount to prioritize the scheduling)
- throw errors on checked exceptions
- Tree visualization 
- Temporal properties