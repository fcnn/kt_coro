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

fun runCoroTest() {
  testCoro(count = 1_000_000)

  runBlocking {
    delay(time = 20)
    workload(n = 1)
  }

  async (CommonPool) {
    workload(n = 10)
  }
}

fun runHttpTest() {
  var http = Http()
  //Http().get("http://gw.codein.net/protocol.html")
  http.post("https://gw.codein.net/echo", "{\"hello world\"}")
  //http.post("test", 0, Reply())
  //http.get("http://gw.codein.net/protocol.html")
  //http.post("https://gw.codein.net/echo", "{\"hello world\"}")
}


fun run(size: Int) {
  var coList = MutableList(size, {
      index -> async { Robot(10000001L + index).run() }
  })
  runBlocking {
    coList.map { co ->
      val robot = co.await()
      println("robot ${robot.uid} exited with status ${robot.status} ...")
    }
  }
}

fun main(args: Array<String>) {
  run(100)
}
