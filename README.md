## Thread
- Lightweight process responsible for executing a task

#### Process vs Thread
- Process is isolated. | Threads share memory.
- Process switching uses interface in operating system | Thread switching does not require to call a operating system and cause an interrupt to the kernel.
- PCB, Stack, Address space  | PCB, TCB, Stack, common Address space
- A thread is a path of execution within a process. A process can contain multiple threads. 
- more time for context switching. | less time for context switching.

#### PCB
- Process Id
- Process state
- Process priority
- Accounting Information
- Program Counter
- CPU Register
- PCB Pointers
- .....



#### TCB
- Thread ID
- Thread State
- CPU Information
- Program Counter
- Register contents
- Thread Priority
- Pointer to process that created this thread
- Pointers to other threads that were created by this thread

** https://www.geeksforgeeks.org/thread-control-block-in-operating-system/?ref=rp


#### Address Space
valid addresses in memory that are available for process.



#### Process in Java
https://www.geeksforgeeks.org/java-lang-process-class-java/



#### Links
- https://www.geeksforgeeks.org/thread-models-in-operating-system/?ref=rp
- https://www.geeksforgeeks.org/thread-in-operating-system/?ref=rp
- https://www.geeksforgeeks.org/mutex-lock-for-linux-thread-synchronization/?ref=rp
- https://www.geeksforgeeks.org/kernel-in-operating-system/?ref=rp
- https://www.geeksforgeeks.org/difference-between-user-level-thread-and-kernel-level-thread/?ref=rp
- https://www.geeksforgeeks.org/difference-between-process-and-kernel-thread/?ref=rp
- https://www.geeksforgeeks.org/difference-between-operating-system-and-kernel/?ref=rp


#### Concurrency vs Parallelism (ToDo)
##### Parallel
- multiple things at same time
- genuine simultaneous execution
##### Concurrency
- multiple things together but not same time 
- appearance of simultaneous execution (by interleaving of processes in time)

#### False Sharing
- [DZone](https://dzone.com/articles/what-false-sharing-is-and-how-jvm-prevents-it)
- https://medium.com/@rukavitsya/what-is-false-sharing-and-how-jvm-prevents-it-82a4ed27da84
- [Jenkov](https://jenkov.com/tutorials/java-concurrency/false-sharing.html)

## Critical Section
a piece of code that accesses a shared resource, usually a variable or data structure.

## Race Condition
a software error that occurs when multiple threads access a shared resource

## Intermediate Program
- Consists of 1 or * race conditions;
- the o/p of program is not <b>deterministic</b> & varies from run to run.

## Mutual Exclusion
no two processes can exist in the critical section at any given point of time

## InterProcess Communication
- coordinate and synchronize between processes 
- ways
  1. Pipes
  2. Signals
  3. Buffers: Message queues etc
  4. Sockets

## Shared memory
- Common memory region from PCB

## AssemblerInstructions
#### ldsub
load/store unsigned instruction byte (locked version of atomic exchange)
#### xchg
atomic exchange

# Concurrency models
1. Parallel Workers
    - Disadvantages
        a. Complexity with Shared state - race conditions, deadlock, non blocking not easy implement
        b. Stateless (worker which re-reads state every time is needed, which can be slow)
        c. Non-deterministic job ordering
2. Assembly Line
    - Reactive event driven systems
    - Actors vs Channels
3. Functional Parallelism

## Mutex
Only thread access resource at a time.

## HappensBefore
Guarntees 1 operation effects are visible to another.

## Read/Write Lock
Multiple readers allowed; writes get exclusive access to resource

## Future/Promise
Promise sets value; future retrieves it from another

## CAS
Atomic operation; update memory only value matches expectations

## Atomics
Variables with in divisible operation, no interference from another thread.

## Single Writer Principle
- One thread writes multiple thread reades. 
- Eliminates race conditions; simplifies concurrency;
- Eliminates lock contention for writes.



