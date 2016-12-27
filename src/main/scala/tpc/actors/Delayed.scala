package tpc.actors

import akka.actor.{Actor, Stash}
import tpc.messages.Resume
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration.FiniteDuration

trait Delayed extends Actor with Stash {

  def suspend(delay: FiniteDuration, resumeState: Receive): Unit = {
    context.system.scheduler.scheduleOnce(delay, self, Resume(resumeState))
    context.become(waiting)
  }

  private def waiting: Receive = {
    case Resume(resumeState) => unstashAll(); context.become(resumeState)
    case _ => stash()
  }
}
