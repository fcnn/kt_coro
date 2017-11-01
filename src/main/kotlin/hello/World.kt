package hello

import kotlinx.coroutines.experimental.*
import java.util.concurrent.atomic.AtomicInteger

suspend fun workload(n: Int): Int {
  delay(time = 1000)
  return n
}

fun run(size: Int) {
  var base = 0
  var coList = MutableList(size, {
      index -> async { Robot(8000000001L + index + base).run() }
  })

  while (coList.size > 0) {
    runBlocking {
      val robot = coList[0].await();
      println("robot ${robot.uid} exited with status ${robot.status} ...")
      coList.removeAt(0)
      //coList.map { co ->
      //  val robot = co.await()
      //  println("robot ${robot.uid} exited with status ${robot.status} ...")
      //}
    }
  }
}

fun main(args: Array<String>) {
  val size = 5000
  run(size)
}
