package com.assignment.throttling.services.actors

import java.util.concurrent.TimeoutException

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import com.assignment.throttling.services.actors.PerTokenSlaMemoizerActor.{EvaluatedSLA, GetSLA, ResetCache}
import com.assignment.throttling.services.{SLA, SlaService}
import akka.pattern.pipe

import scala.concurrent.duration._
import scala.language.postfixOps


/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 11 Feb 2017
  *
  */
object PerTokenSlaMemoizerActor {
  def props(slaService: SlaService, token: String): Props = Props(new PerTokenSlaMemoizerActor(slaService, token))

  private[services] final case object GetSLA

  private[services] final case class EvaluatedSLA(sla: SLA)

  private final case object ResetCache
}

class PerTokenSlaMemoizerActor private(slaService: SlaService, token: String) extends Actor with ActorLogging {
  import context._

  private var cachedSla : Option[SLA] = None

  //===============================================
  //                Behavior
  //===============================================

  override def receive: Receive = {

    case GetSLA =>
      become(waitingForSla)
      parent forward SlaMemoizerActor.Unauthorized
      slaService.getSlaByToken(token) map PerTokenSlaMemoizerActor.EvaluatedSLA pipeTo self


    case ResetCache =>
      cachedSla = None

  }

  private def waitingForSla: Receive = {
    case GetSLA =>
      parent forward SlaMemoizerActor.Unauthorized

    case EvaluatedSLA(sla) =>
      cachedSla = Some(sla)
      become(slaComputed)
      scheduleReset()


    case ResetCache =>
      cachedSla = None
      become(receive)
  }

  private def slaComputed: Receive = {
    case GetSLA =>
      parent forward SlaMemoizerActor.ComputedSLA(cachedSla.get)


    case ResetCache =>
      cachedSla = None
      become(receive)
  }


  //===============================================
  //                Schedulers
  //===============================================

  private def scheduleReset() = {
    system.scheduler.scheduleOnce(
      delay = 5 hours,               //TODO: make me configurable
      receiver = self,
      message = ResetCache
    )
  }

  //TODO: might be implemented more precisely
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: TimeoutException => Restart
      case t =>
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

}
