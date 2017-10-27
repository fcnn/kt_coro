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


fun runRoboTest() {
  var robotList = List<Robot>( 4, {
    index -> Robot(201800000L + index)
  })

  robotList.map { robot ->
    robot.run()
  }
  robotList.map { robot ->
    robot.stop()
  }
}

fun main(args: Array<String>) {
  println("Start")

  runRoboTest()

  println("Stop")
}
