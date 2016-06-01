package worker

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.event.Logging
import Master._
import Worker._
import com.typesafe.config.ConfigFactory

object Master {
  sealed trait MasterEvent
  case object StartWorking extends  MasterEvent
  case object RequestReport extends  MasterEvent
  case class OrderMagnitudeCounts(
                              overOneMilliCount: Long = 0,
                              over50MillisCount: Long = 0,
                              over100MillisCount: Long = 0,
                              over200MillisCount: Long = 0,
                              over500MillisCount: Long = 0,
                              over1SecCount: Long = 0,
                              over5SecsCount: Long = 0
                            )
  case class Aggregates(
                         meanTime: Long = 0,
                         minTime: Long = 0,
                         maxTime: Long = 0,
                         count: Long = 0,
                         omCounts: OrderMagnitudeCounts = OrderMagnitudeCounts()
                       ) {
    val oneMillion = 1000000
    val oneBillion = 1000 * oneMillion

    def update(time: Long) = {
      def updateCount(threshold: Int, count: Long) = if (time > threshold) count + 1 else count

      val counts = OrderMagnitudeCounts(
        updateCount(oneMillion, omCounts.overOneMilliCount),
        updateCount(50 * oneMillion, omCounts.over50MillisCount),
        updateCount(100 * oneMillion, omCounts.over100MillisCount),
        updateCount(200 * oneMillion, omCounts.over200MillisCount),
        updateCount(500 * oneMillion, omCounts.over500MillisCount),
        updateCount(oneBillion, omCounts.over1SecCount),
        updateCount(5 * oneBillion, omCounts.over5SecsCount)
      )
      Aggregates(
        (meanTime * count + time) / (count + 1),
        if (minTime == 0 || time < minTime) time else minTime,
        if (time > maxTime) time else maxTime,
        count + 1,
        counts
      )
    }
  }
}

class Master extends Actor {

  case class WorkerLives(ref: ActorRef, lives: Int) {
    def loseOneLife = copy(lives = lives - 1)
  }

  val config = ConfigFactory.load()
  val log = Logging(context.system, this)
  val r = scala.util.Random
  val initialNumWorkers = config.getInt("workers")
  val lives = config.getInt("lives")

  def init: Receive = {
    case StartWorking =>
      val workers = (1 to initialNumWorkers).foldLeft(Map.empty[ActorRef, Int]) { (m, num) =>
        m + (context.actorOf(Props[Worker]) -> r.nextInt(lives))
      }
      workers foreach { case (ref, lives) => ref ! BuildRandomHistory }
      context.become(creatingWork(workers))
  }

  def creatingWork(
      workers: Map[ActorRef, Int],
      report: Aggregates = Aggregates()
    ): Receive = {

    case WorkDone(execTime) => {
      val remainingLives = workers(sender)
      val newWorkers = if (remainingLives == 0) {
        sender ! PoisonPill
        val newWorker = context.actorOf(Props[Worker])
        newWorker ! BuildRandomHistory
        workers - sender + (newWorker -> r.nextInt(lives))
      } else {
        sender ! BuildRandomHistory
        workers + (sender -> (remainingLives - 1))
      }
      context.become(
        creatingWork(newWorkers, report.update(execTime))
      )
    }
    case RequestReport =>
      sender ! report
    case _ => log.warning("received unknown message")
  }

  def receive = init
}
