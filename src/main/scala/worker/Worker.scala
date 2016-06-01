package worker

import akka.actor.Actor
import akka.event.Logging
import Worker.{BuildRandomHistory, WorkDone}
import com.typesafe.config.ConfigFactory

object Worker {
  sealed trait WorkerEvent
  case object BuildRandomHistory extends WorkerEvent
  case class WorkDone(execTime: Long) extends WorkerEvent
}

class Worker extends Actor {
  val config = ConfigFactory.load()
  val log = Logging(context.system, this)
  val r = scala.util.Random
  val listSize = config.getInt("listSize")

  private def randomNums(size: Int) = List.fill(size)(r.nextInt(1000))

  def receive = {
    def _receive(collectible: List[Int], history: List[List[Int]]): Receive = {
      case BuildRandomHistory =>
        val start = System.nanoTime
        // This is likely to cause a GC as it is the only place where we allocate larger objects
        val newHistory = randomNums(r.nextInt(listSize)) :: history
        val garbage = randomNums(listSize) // Can be collected right away
        val execTime = System.nanoTime - start
        sender ! WorkDone(execTime)
        context.become(_receive(garbage, newHistory))
      case _      => log.warning("received unknown message")
    }
    _receive(Nil, Nil)
  }
}
