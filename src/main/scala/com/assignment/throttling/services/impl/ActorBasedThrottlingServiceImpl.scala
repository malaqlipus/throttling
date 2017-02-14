package com.assignment.throttling.services.impl

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.assignment.throttling.services.actors.ThrottlingActor
import com.assignment.throttling.services.actors.ThrottlingActor.IsRequestAllowed
import com.assignment.throttling.services.{SlaService, ThrottlingService}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 09 Feb 2017
  *
  */
class ActorBasedThrottlingServiceImpl(
  val graceRps:Int,
  val slaService: SlaService
) extends ThrottlingService {

  //TODO: make me configurable or decide on final value, currently too big
  implicit val timeout = Timeout(200 milliseconds)

  val system = ActorSystem.create()

  val throttlingActor: ActorRef = system.actorOf(ThrottlingActor.props(slaService, graceRps), "throttlingActor")

  override def isRequestAllowed(token: Option[String]): Future[Boolean] = {

    (throttlingActor ? IsRequestAllowed(token)).mapTo[Boolean]
  }

}
