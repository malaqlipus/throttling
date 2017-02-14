package com.assignment.throttling.services.actors

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit, TestProbe}
import com.assignment.throttling.services.SLA
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, OneInstancePerTest, WordSpecLike}

import scala.language.postfixOps
/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 11 Feb 2017
  *
  */
class RpsCounterActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender with DefaultTimeout
  with WordSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures with OneInstancePerTest with MockFactory {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val graceRpsCount = 10000

  val parent = TestProbe()
  val actorSut = parent.childActorOf(RpsCounterActor.props())


  "RpsCounter actor" must {

    "count rps correctly after 1 second break in messages" in {

      sendMessageAndExpectResult(isRequestAllowed = true, requestCount = graceRpsCount)
      sendMessageAndExpectResult(isRequestAllowed = false)

      Thread.sleep(1000)

      sendMessageAndExpectResult(isRequestAllowed = true, requestCount = graceRpsCount)
      sendMessageAndExpectResult(isRequestAllowed = false)
    }


    "allow 10% more after > 100 milliseconds, but in same second" in {

      sendMessageAndExpectResult(isRequestAllowed = true, requestCount = graceRpsCount)
      sendMessageAndExpectResult(isRequestAllowed = false)

      Thread.sleep(100)

      sendMessageAndExpectResult(isRequestAllowed = true, requestCount = graceRpsCount/10)
      sendMessageAndExpectResult(isRequestAllowed = false)

      Thread.sleep(1000)

      sendMessageAndExpectResult(isRequestAllowed = true, requestCount = graceRpsCount)
    }

  }

  private def sendMessageAndExpectResult(isRequestAllowed: Boolean, requestCount: Int = 1) = {
    (1 to requestCount).par.foreach { _ =>
      parent.send(actorSut, RpsCounterActor.IsAllowedForSLA( SLA("_", graceRpsCount)) )
    }
    awaitAssert(() => {
      val msgs = parent.receiveN(requestCount) map {_.asInstanceOf[ThrottlingActor.Result]}
      assert(msgs.forall( msg => msg.res == isRequestAllowed))
    })

  }

}
