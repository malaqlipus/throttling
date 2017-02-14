package com.assignment.throttling.services.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.assignment.throttling.services.SLA
import com.assignment.throttling.services.actors.RpsCounterActor.{IsAllowedForSLA, Result}

import scala.collection.mutable
import scala.language.postfixOps

/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 13 Feb 2017
  *
  */
object RpsCounterActor {
  def props(): Props = Props(new RpsCounterActor())

  private[services] final case class IsAllowedForSLA(sla: SLA)

  private[services] final case object Unauthorized

  private[services] final case class Result(res: Boolean)
}


class RpsCounterActor extends Actor with ActorLogging {

  import context._

  private val countersLedger: mutable.Map[String, ActorRef] = new mutable.HashMap[String, ActorRef]()

  override def receive: Receive = {

    case IsAllowedForSLA(sla) =>
      def buildNewPerUserRpsCounterActor: ActorRef = {
        actorOf(PerUserRpsCounterActor.props(sla), name = s"PerUserRpsCounterActor_${sla.user}")
      }

      val usersQueue = countersLedger.getOrElseUpdate(sla.user, buildNewPerUserRpsCounterActor)
      usersQueue forward PerUserRpsCounterActor.IncRps

    case Result(res) =>
      parent forward ThrottlingActor.Result(res)

  }


}
