# Before public release
- Lock.tryLock
- More examples
- Documentation

# Before I consider 'usable':
- Injection on java rt classes
- Random execution with max time 
  - random exploration for when tests are taking too long
- wait / notify / lock.condition coordination
- random / sleep detection
  - random should be seeded or bypassed 
- Checkpoint matcher's should operate on CheckpointDescriptors
- repeated state count (if the system as a whole is not making progress)
  - hash over threadstates to try to detect a 'loop'

# Nice to haves
- Resume actors with action
  - for instance, spurious wakeup, exception thrown, etc
- Recorded output and re-run with recorded output
- Better throughput (better CPU usage) 
  - maybe we can swap the semaphore for LockSupport.park / unpark?
- Data race analysis (look at the bytecode, see shared objects and generate checkpoints based on those)
- better support for loops (eg: use the loopcount to prioritize the scheduling)
- throw errors on checked exceptions 
- Tree visualization 