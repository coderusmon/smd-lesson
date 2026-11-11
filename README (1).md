# CoroutinesLab

Practice work, coroutines. Kotlin/JVM, kotlinx-coroutines-core 1.10.2.

All code is in `src/main/kotlin/Main.kt`. Run with `./gradlew run`.

## Part B

<details>
<summary>Task 1</summary>

video:

1. Run main. Output is Start, B done, A done.
2. Change delay() to Thread.sleep() in both coroutines and run again. Output is Start, A done, B done.

Thread.sleep() blocks the main thread, so B can not start until A is finished.

</details>

<details>
<summary>Task 2</summary>

video:

1. n = 3: try 1 failed, try 2 failed, User: Ada.
2. n = 2: try 1 failed, try 2 failed, Server is down, show cached user.
3. Call fetchName() from a normal fun and build.

Error:

```
Main.kt:69:5: error: suspend function 'suspend fun fetchName(): String?' can only be called from a coroutine or another suspend function.
```

</details>

<details>
<summary>Task 3</summary>

video:

1. With cancelAndJoin(): tick 0, tick 1, tick 2, cleanup, isCancelled=true.
2. Change cancelAndJoin() to join(): all 5 ticks, cleanup, isCancelled=false.

</details>

<details>
<summary>Task 4</summary>

video:

1. One by one, about 200 ms.
2. Two async and await later, about 100 ms.
3. await right after each async, about 200 ms.

In the third case await() waits for the first result before the second async is even started. So they do not run together and it is the same as one by one.

</details>

<details>
<summary>Task 5</summary>

video:

1. Default: DefaultDispatcher-worker-1
2. IO: DefaultDispatcher-worker-2
3. Unconfined: main
4. MyThread before and after delay, then close()
5. no dispatcher: main

</details>

## Structured concurrency in this repo

```kotlin
fun main() = runBlocking {      // parent of everything, main thread waits here
    task1()
    task3()
    ...
}

suspend fun task1() = coroutineScope {   // child scope
    launch { ... }    // A, child of coroutineScope
    launch { ... }    // B, child of coroutineScope
    println("Start")
}                     // returns only after A and B are done

// I never use GlobalScope. Every launch and async is inside coroutineScope,
// and coroutineScope is called from runBlocking. So it is one tree:
// runBlocking -> coroutineScope of the task -> launch / async.
//
// The parent waits for children, that is why task 2 starts only after
// A and B from task 1 are finished.
// In task 3 I cancel one child with cancelAndJoin(), its finally runs
// and other coroutines are not affected.
// In task 4 if loadName() throws, the scope cancels loadAge() too.
// In task 5 launch(Dispatchers.IO) changes only the thread, the parent is the same.
// The thread from newSingleThreadContext is not in this tree, so I close it by hand.
```

## Part A

**Q1. newSingleThreadContext and Dispatchers.Unconfined**

newSingleThreadContext creates a new thread for coroutines. The coroutine is on this thread before delay() and after delay(). When I finish I must call close(), because the thread is not stopped automatically.

Unconfined does not have its own thread. The coroutine starts on the thread of the caller. After delay() it continues on the thread that resumed it, so it can be a different thread.

```kotlin
launch(Dispatchers.Unconfined) {
    println(Thread.currentThread().name)   // main
    delay(100)
    println(Thread.currentThread().name)   // kotlinx.coroutines.DefaultExecutor
}

val ctx = newSingleThreadContext("MyThread")
launch(ctx) {
    println(Thread.currentThread().name)   // MyThread
    delay(100)
    println(Thread.currentThread().name)   // MyThread
}.join()
ctx.close()
```

**Q2. async and launch**

launch returns Job and has no result. async returns Deferred<T> and has a result. For launch I use join(), it only waits. For async I use await(), it waits and returns the value.

**Q3. Coroutine, suspend and block**

A coroutine is code that can stop and continue later. It is lighter than a thread. Block means the thread is stopped and does nothing. Suspend means only the coroutine is stopped and the thread can do other work.

If two coroutines on one thread call delay(100), both wait at the same time and it takes about 100 ms. With Thread.sleep(100) the first one keeps the thread, the second waits for it, and it takes about 200 ms.

**Q4. suspend keyword**

It says that the function can be paused and resumed. It does not create a new thread. A suspend function can be called only from a coroutine or from another suspend function. The compiler adds a hidden parameter Continuation, it saves where the function stopped.

**Q5. Concurrency and parallelism**

Concurrency is when several tasks are in progress and they take turns. Parallelism is when tasks run at the same moment on different cores. Coroutines on one thread give concurrency. Parallelism needs several threads and several cores.

**Q6. Structured concurrency**

A coroutine is started inside a scope and it is a child of the coroutine that started it. So there is a tree of parents and children.

- The parent does not finish until all children finish.
- If the parent is cancelled, all children are cancelled.
- If a child fails, the parent fails and the other children are cancelled.

**Q7. Builders**

- launch returns Job, does not block.
- async returns Deferred, does not block.
- runBlocking returns the result of the block and blocks the thread.
- withContext returns the result of the block, suspends, used to change dispatcher.
- coroutineScope returns the result of the block, suspends until children finish.

runBlocking on the Android UI thread blocks it, so the app freezes and can show ANR.

**Q8. CoroutineScope and CoroutineContext**

CoroutineContext is a set of elements of a coroutine: Job, dispatcher, name, exception handler. CoroutineScope keeps a context and launch and async are called on it. Scope is when the coroutine lives, dispatcher is where it runs.

If I specify nothing, the coroutine takes the dispatcher of the parent. If there is no dispatcher in the parent, Dispatchers.Default is used.

**Q9. Default, IO, Main**

- Default: CPU work, for example sorting a big list. Avoid blocking calls like network.
- IO: reading files, database, network. Avoid heavy calculations.
- Main: updating UI, for example setting text. Avoid long work, the UI freezes.

**Q10. viewModelScope.launch { withContext(Dispatchers.IO) { ... } }**

viewModelScope.launch starts a coroutine on the main thread. withContext(Dispatchers.IO) moves the block to an IO thread and the main thread is free while it works. The code inside the block runs on the IO thread. After the block the coroutine is back on the main thread, so I can update UI there.

When the ViewModel is cleared, viewModelScope is cancelled and this coroutine is cancelled too.
