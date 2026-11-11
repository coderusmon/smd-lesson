import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

fun main() = runBlocking {
    println("Task 1")
    task1()

    println("\nTask 2, n = 3")
    calls = 0
    showUser(3)
    println("\nTask 2, n = 2")
    calls = 0
    showUser(2)

    println("\nTask 3")
    task3()

    println("\nTask 4")
    task4()

    println("\nTask 5")
    task5()
}

suspend fun task1() = coroutineScope {
    launch {
        delay(200)
        println("A done")
    }
    launch {
        delay(100)
        println("B done")
    }
    println("Start")
}

var calls = 0

suspend fun fetchName(): String? {
    delay(100)
    calls++
    if (calls <= 2) return null
    return "Ada"
}

suspend fun showUser(n: Int) {
    for (k in 1..n) {
        val name = fetchName()
        if (name != null) {
            println("User: $name")
            return
        }
        println("try $k failed")
    }
    println("Server is down, show cached user")
}

suspend fun task3() = coroutineScope {
    val job = launch {
        try {
            for (i in 0..4) {
                println("tick $i")
                delay(100)
            }
        } finally {
            println("cleanup")
        }
    }
    delay(250)
    job.cancelAndJoin()
    // job.join()
    println("isCancelled=${job.isCancelled}")
}

suspend fun loadName(): String {
    delay(100)
    return "Ada"
}

suspend fun loadAge(): Int {
    delay(100)
    return 36
}

suspend fun task4() = coroutineScope {
    val t1 = measureTimeMillis {
        val name = loadName()
        val age = loadAge()
        println("$name $age")
    }
    println("one by one: $t1 ms")

    val t2 = measureTimeMillis {
        val name = async { loadName() }
        val age = async { loadAge() }
        println("${name.await()} ${age.await()}")
    }
    println("async: $t2 ms")

    val t3 = measureTimeMillis {
        val name = async { loadName() }.await()
        val age = async { loadAge() }.await()
        println("$name $age")
    }
    println("await right after async: $t3 ms")
}

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
suspend fun task5() = coroutineScope {
    launch(Dispatchers.Default) {
        println("Default: " + Thread.currentThread().name)
    }.join()

    launch(Dispatchers.IO) {
        println("IO: " + Thread.currentThread().name)
    }.join()

    launch(Dispatchers.Unconfined) {
        println("Unconfined: " + Thread.currentThread().name)
    }.join()

    val ctx = newSingleThreadContext("MyThread")
    launch(ctx) {
        println("before delay: " + Thread.currentThread().name)
        delay(100)
        println("after delay: " + Thread.currentThread().name)
    }.join()
    ctx.close()

    launch {
        println("no dispatcher: " + Thread.currentThread().name)
    }.join()
}
