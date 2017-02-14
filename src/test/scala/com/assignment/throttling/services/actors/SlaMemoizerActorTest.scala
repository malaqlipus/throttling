package com.assignment.throttling.services.actors

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit, TestProbe}
import com.assignment.throttling.services.{SLA, SlaService}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, OneInstancePerTest, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 13 Feb 2017
  *
  */
class SlaMemoizerActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender with DefaultTimeout
  with WordSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures with OneInstancePerTest with MockFactory {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val parent = TestProbe()

  val graceRpsCount = 100
  val slaToReturn = SLA("someUserName", 999)
  val someToken = Some("NiceToken098098098")

  "SlaMemoizerActor" must {

    "return grace rps for unauthorized" in {
      val slaService: SlaService = mock[SlaService]
      val actorSut = parent.childActorOf(SlaMemoizerActor.props(slaService, graceRpsCount))

      (slaService.getSlaByToken _).expects(*).repeat(0) //no calls to service for unauthorized

      parent.send(actorSut, SlaMemoizerActor.GetSLA(None))

      val rpsFromSla = parent.expectMsgPF[Int](){
        case ThrottlingActor.ComputedSLA(defaultSla) => defaultSla.rps
      }
      assert(rpsFromSla == graceRpsCount)
    }


    "only one call to sla service even if asked many times, same result as for unauthorized" in {
      val slaService: SlaService = mock[SlaService]
      val actorSut = parent.childActorOf(SlaMemoizerActor.props(slaService, graceRpsCount))

      (slaService.getSlaByToken _).expects(someToken.get).onCall {_: String => Future {
          Thread.sleep(500)
          slaToReturn
        }
      }.once()

      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))
      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))
      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))
      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))
      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))

      val msgs = parent.receiveN(5) map {_.asInstanceOf[ThrottlingActor.ComputedSLA]}

      assert(msgs.size == 5)
      assert(msgs.forall{ msg => msg.sla.rps == graceRpsCount})
    }


    "once sla is computed it always returned" in {
      val slaService: SlaService = stub[SlaService]
      val actorSut = parent.childActorOf(SlaMemoizerActor.props(slaService, graceRpsCount))

      (slaService.getSlaByToken _).when(someToken.get).onCall{_: String =>
        Future successful slaToReturn
      }.once()

      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))
      Thread.sleep(5) //giving a chance for Future to complete

      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))
      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))
      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))
      parent.send(actorSut, SlaMemoizerActor.GetSLA(someToken))

      val msgs = parent.receiveN(5) map {_.asInstanceOf[ThrottlingActor.ComputedSLA]}

      assert(msgs.size == 5)
      //all except first should contain computed sla from service and
      assert(msgs.drop(1).forall{ msg => msg.sla.rps == 999})
    }

  }

}
