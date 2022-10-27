# Before public release
- Lock.tryLock
- More examples
- Documentation

# Before I consider 'usable':
- Injection on java rt classes
- Random execution with max time 
  - random exploration for when tests are taking too long
- Better error output (thread monitor / lock status)
- Better lock / monitor information
  - monitors currently don't even hold the monitor class information
- wait / notify / lock.condition coordination
- random / sleep detection
  - random should be seeded or bypassed 
- Resume actors with action
  - for instance, spurious wakeup, exception thrown, etc
- Checkpoint matcher's should operate on CheckpointDescriptors
- Standard method checkpoints
- Check if we can eliminate some of the default checkpoints
- repeated state count (if the system as a whole is not making progress)

# Nice to haves
- Recorded output and re-run with recorded output
- Better throughput (better CPU usage) 
- Data race analysis (look at the bytecode, see shared objects and generate checkpoints based on those)
- better support for loops (eg: use the loopcount to prioritize the scheduling)
- throw errors on checked exceptions 
- Tree visualization 