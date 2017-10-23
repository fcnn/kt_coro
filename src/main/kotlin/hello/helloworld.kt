package hello

import kotlinx.coroutines.experimental.*
import java.util.concurrent.atomic.AtomicInteger

fun getGreeting(): String {
  val words = mutableListOf<String>()
  words.add("Hello,")
  words.add("world!")

  return words.joinToString(separator = " ")
}

fun thread_ver(count: Int): Int {
  val c = AtomicInteger()

  for (i in 1..count)
    kotlin.concurrent.thread(start = true) {
      c.addAndGet(i)
    }

  println(c.get())
  return c.get()
}

fun coro_ver(count: Int): Int {
  val deferred = (1..count).map { n ->
    async (CommonPool) {
      n
    }
  }
  var sum: Int = 0
  runBlocking {
    sum = deferred.sumBy { it.await() }
  }
  println("Sum: $sum")
  return sum
}

suspend fun workload(n: Int): Int {
  delay(1000)
  return n
}

fun main(args: Array<String>) {
  println("Start")

  coro_ver(1_000_000)

  runBlocking {
    delay(20)
    workload(1)
  }

  async (CommonPool) {
    workload(10)
  }

  println("Stop")
}
