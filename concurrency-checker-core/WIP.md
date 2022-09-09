# Currently working on:
- Manual checkpoints
- Tracking method calls
- java.util.concurrent specific classes and methods
  - easy ones are atomicX and lock
- wait and notify
  - fork off wait to mimic spurious wakeup
- check if, for synchronized methods, its enough to unset the modifier and manually inject the synchronized block
- better configuration of checkpoint injection
- use records for CheckpointReached

# planned / done checkpoint types
- manual - missing stack manipulation of details
- field access - missing pushing field details to stack
- synchronized blocks 
- method calls
  - try to find synchronized methods in runtime due to polymorphic invocation
- array load / store
- atomicX
- 