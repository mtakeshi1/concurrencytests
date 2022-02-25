# Before I consider 'usable':

- @ManualCheckpoints
- Fork to another jvm 
- Fork agent attacher to another jvm (unless allowSel)
- Random execution with max time 
- Better error output (thread monitor / lock status)
- Better lock / monitor information
- Recoded output and re-run with recorded output
- Polymorphic checkpoints
- better checkpoint names and details
- 

# Nice to haves

- Better throughput (better CPU usage) 
- Data race analysis (look at the bytecode, see shared objects and generate checkpoints based on those)
- better support for loops (eg: use the loopcount to prioritize the scheduling)
- off-heap graph (and better memory usage)