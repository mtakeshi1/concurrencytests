# Currently working on:
- Tracking method calls
- java.util.concurrent specific classes and methods
  - easy ones are atomicX and lock
- wait and notify
  - fork off wait to mimic spurious wakeup
- checkpoint location shouldn't be ambiguous


# planned / done checkpoint types
- manual - missing stack manipulation of details
- field access - missing pushing field details to stack


# open questions
- should we persist / serialize thread state / details with the checkpoint?
- should we special case java.util.concurrent.Lock?
- how to deal with conditional Lock.tryLock
- 