# Before I consider 'usable':
- Random execution with max time 
  - random exploration for when tests are taking too long
- Better error output (thread monitor / lock status)
- Better lock / monitor information
  - monitors currently don't even hold the monitor class information
- Recorded output and re-run with recorded output
- better checkpoint names and details 
- wait / notify / lock.condition coordination
- random / sleep detection
  - random should be seeded or bypassed 
- Resume actors with action
  - for instance, spurious wakeup, exception thrown, etc

# Nice to haves

- Better throughput (better CPU usage) 
- Data race analysis (look at the bytecode, see shared objects and generate checkpoints based on those)
- better support for loops (eg: use the loopcount to prioritize the scheduling)
- throw errors on checked exceptions 
- Tree visualization 