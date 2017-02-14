package com.assignment.throttling.services.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.assignment.throttling.services.SLA
import com.assignment.throttling.services.actors.PerUserRpsCounterActor.{AcceptAdditionalRps, IncRps, ResetRps}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 08 Feb 2017
  *
  */

object PerUserRpsCounterActor {
  def props(sla: SLA): Props = Props(new PerUserRpsCounterActor(sla.user, sla.rps))

  private[services] final case object IncRps

  private[services] final case object ResetRps

  private[services] final case object AcceptAdditionalRps
}

/**
  * There is slight problem with this implementation - delay before messages to `self` is processed
  * Two possible solutions:
  * 1. rewrite with one `receive` and Atomic counters (I've done this, if interested, just ask)
  * 2. introduce priority queue for this actor
  */
class PerUserRpsCounterActor private (
  private val username: String,
  private val defaultSlaRps: Int
) extends Actor with ActorLogging {
  import context._

  private var count: Int = 0
  private var allowedSlaRps: Int = defaultSlaRps

  //===============================================
  //                Behavior
  //===============================================

  override def receive: Receive = {
    case IncRps =>
      count += 1
      parent forward RpsCounterActor.Result(true)
      scheduleReset()
      become(normal)

    case ResetRps =>
      count = 0
      allowedSlaRps = defaultSlaRps

  }

  def normal: Receive = {
    case IncRps =>
      if (count + 1 <= allowedSlaRps) {
        count += 1
        parent forward RpsCounterActor.Result(true)
      } else {
        become(silent)
        scheduleAcceptAdditional()
        parent forward RpsCounterActor.Result(false)
      }

    case ResetRps =>
      resetAndBecomeDefault()

  }

  def silent: Receive = {
    case IncRps =>
      parent forward RpsCounterActor.Result(false)

    case AcceptAdditionalRps =>
      allowedSlaRps += allowedSlaRps / 10
      become(acceptAdditional)

    case ResetRps =>
      resetAndBecomeDefault()
  }

  def acceptAdditional: Receive = {
    case IncRps =>
      count += 1
      parent forward RpsCounterActor.Result(count <= allowedSlaRps)

    case ResetRps =>
      resetAndBecomeDefault()
  }

  private def resetAndBecomeDefault(): Unit = {
    count = 0
    allowedSlaRps = defaultSlaRps
    become(receive)
  }

  //===============================================
  //                Schedulers
  //===============================================

  private def scheduleReset() = {
    system.scheduler.scheduleOnce(
      delay = 1 second,
      receiver = self,
      message = ResetRps
    )
  }

  private def scheduleAcceptAdditional() = {
    system.scheduler.scheduleOnce(
      100 milliseconds,
      receiver = self,
      message = AcceptAdditionalRps
    )
  }
}
