package hello

import kotlinx.coroutines.experimental.*
import java.util.concurrent.atomic.AtomicInteger

fun testCoro(count: Int): Int {
  val deferred = (1..count).map { n ->
    async (CommonPool) {
      n
    }
  }
  var sum = 0
  runBlocking {
    sum = deferred.sumBy { it.await() }
  }
  println("Sum: $sum")
  return sum
}

suspend fun workload(n: Int): Int {
  delay(time = 1000)
  return n
}

fun main(args: Array<String>) {
  println("Start")

  testCoro(count = 1_000_000)

  runBlocking {
    delay(time = 20)
    workload(n = 1)
  }

  async (CommonPool) {
    workload(n = 10)
  }
  Http.main(args)

  println("Stop")
}
