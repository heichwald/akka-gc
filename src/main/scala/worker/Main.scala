package worker

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.pattern.ask
import akka.event.Logging

import scala.concurrent.duration._
import Master._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  def main(args: Array[String]): Unit = {
    val start = System.currentTimeMillis
    val system = ActorSystem("WorkersSystem")
    val log = Logging.getLogger(system, this)
    val config = ConfigFactory.load()

    val master = system.actorOf(Props[Master])
    system.scheduler.scheduleOnce(config.getInt("testDuration") seconds) {
      implicit val timeout = Timeout(10 seconds)
      val f = master ? RequestReport
      f foreach { case report: Aggregates =>
        log.info(s"Throughput: {} /sec", report.count * 1000 / (System.currentTimeMillis - start))
        log.info(s"Mean: {} micro sec", report.meanTime / 1000)
        log.info(s"Min: {} micro sec", report.minTime / 1000)
        log.info(s"Max: {} ms", report.maxTime / 1000000)
        log.info("> 1ms and <= 50 ms: {}", report.omCounts.overOneMilliCount - report.omCounts.over50MillisCount)
        log.info("> 50 ms and <= 100 ms: {}", report.omCounts.over50MillisCount - report.omCounts.over100MillisCount)
        log.info("> 100 ms and <= 200 ms: {}", report.omCounts.over100MillisCount - report.omCounts.over200MillisCount)
        log.info("> 200 ms and <= 500 ms: {}", report.omCounts.over200MillisCount - report.omCounts.over500MillisCount)
        log.info("> 500 ms and <= 1 sec: {}", report.omCounts.over500MillisCount - report.omCounts.over1SecCount)
        log.info("> 1 sec and <= 5 sec: {}", report.omCounts.over1SecCount - report.omCounts.over5SecsCount)
        log.info("> 5 sec: {}", report.omCounts.over5SecsCount)
        system.terminate()
      }
      f recover {
        case e: Exception =>
          log.error(e, "fail to get report")
          system.terminate()
      }
    }
    master ! StartWorking
  }
}
