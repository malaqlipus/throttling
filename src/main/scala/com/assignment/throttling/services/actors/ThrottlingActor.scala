package com.assignment.throttling.services.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.assignment.throttling.services.actors.ThrottlingActor.{ComputedSLA, IsRequestAllowed, Result}
import com.assignment.throttling.services.{SLA, SlaService}


/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 08 Feb 2017
  *
  */
object ThrottlingActor {
  def props(slaService: SlaService, graceRps: Int): Props = Props(new ThrottlingActor(slaService, graceRps))

  private[services] final case class IsRequestAllowed(tokenOpt: Option[String])

  private[services] final case class ComputedSLA(sla: SLA)

  private[services] final case class Result(res: Boolean)

}

class ThrottlingActor private(slaService: SlaService, graceRps: Int) extends Actor with ActorLogging {
  import context._

  private val slaMemoizerActor: ActorRef = actorOf(SlaMemoizerActor.props(slaService, graceRps))
  private val rpsCounterActor: ActorRef = actorOf(RpsCounterActor.props())

  def receive: Receive = {

    case IsRequestAllowed(token) =>
      slaMemoizerActor forward SlaMemoizerActor.GetSLA(token)

    case ComputedSLA(sla) =>
      rpsCounterActor forward RpsCounterActor.IsAllowedForSLA(sla)

    case Result(res) =>
      sender ! res

  }

  //TODO: think about supervision strategy

}