package com.assignment.throttling.services.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.assignment.throttling.services.actors.SlaMemoizerActor.{ComputedSLA, GetSLA, Unauthorized}
import com.assignment.throttling.services.{SLA, SlaService}

import scala.collection.mutable
import scala.language.postfixOps

/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 12 Feb 2017
  *
  */
object SlaMemoizerActor {
  def props(slaService: SlaService, graceRps: Int): Props = Props(new SlaMemoizerActor(slaService, graceRps))

  private[services] final case class GetSLA(token: Option[String])

  private[services] final case class ComputedSLA(sla: SLA)

  private[services] final case object Unauthorized
}

class SlaMemoizerActor private(slaService: SlaService, graceRps: Int) extends Actor with ActorLogging {

  import context._

  private val memoizersLedger: mutable.Map[String, ActorRef] = new mutable.HashMap[String, ActorRef]()

  private val defaultSla: SLA = SLA("_", graceRps)

  override def receive: Receive = {

    case GetSLA(Some(token)) =>
      def buildNewPerTokenSlaMemoizerActor: ActorRef = {
        actorOf(PerTokenSlaMemoizerActor.props(slaService, token), name = s"PerTokenSlaMemoizerActor$token")
      }

      val usersQueue = memoizersLedger.getOrElseUpdate(token, buildNewPerTokenSlaMemoizerActor)
      usersQueue forward PerTokenSlaMemoizerActor.GetSLA


    case GetSLA(None) =>
      parent forward ThrottlingActor.ComputedSLA(defaultSla)

    case Unauthorized =>
      parent forward ThrottlingActor.ComputedSLA(defaultSla)

    case ComputedSLA(sla) =>
      parent forward ThrottlingActor.ComputedSLA(sla)

  }

}