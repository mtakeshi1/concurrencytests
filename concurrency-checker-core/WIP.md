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
- checkpoint location shouldn't be ambiguous


# planned / done checkpoint types
- manual - missing stack manipulation of details
- field access - missing pushing field details to stack
- synchronized blocks 
- method calls
  - try to find synchronized methods in runtime due to polymorphic invocation
- array load / store
- atomicX


# open questions
- should we persist / serialize thread state / details with the checkpoint?
- should we special case java.util.concurrent.Lock?
- decide what to do with new Thread() { public void run() {} }
- 