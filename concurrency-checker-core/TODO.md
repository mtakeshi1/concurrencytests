# Before I consider 'usable':
- Random execution with max time 
- Better error output (thread monitor / lock status)
- Better lock / monitor information
- Recoded output and re-run with recorded output
- better checkpoint names and details 
- wait / notify / lock.condition coordination
- random / sleep detection
 

# Nice to haves

- Better throughput (better CPU usage) 
- Data race analysis (look at the bytecode, see shared objects and generate checkpoints based on those)
- better support for loops (eg: use the loopcount to prioritize the scheduling)
- throw errors on checked exceptions 
- Tree visualization 