# Currently working on:
- Removing old code
- off-heap graph - allows forking
- Fork to another jvm
  - we could export the classes to the other jvm via standard classpath mechanism
- java.util.concurrent specific classes and methods
  - easy ones are atomicX and lock

# planned / done checkpoint types
- field access - missing pushing field details to stack
- missing inspecting stack to fill in monitor type
- wait and notify
- checkpoint location shouldn't be ambiguous

# open questions
- should we persist / serialize thread state / details with the checkpoint?
- should we special case java.util.concurrent.Lock?
  - how about other concurrency 'primitives'?
- how to deal with conditional Lock.tryLock
- 