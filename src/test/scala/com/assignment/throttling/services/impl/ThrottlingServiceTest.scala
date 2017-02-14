package com.assignment.throttling.services.impl

import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

import com.assignment.throttling.services.{SLA, SlaService}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps


/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 14 Jan 2017
  *
  */
class ThrottlingServiceTest extends FlatSpec with MockFactory {

  behavior of "Throttling service"

  val tokensCount = 200 //we assume one token for one user
  val rps = 100
  val time = 5 seconds

  it should "... load test" in {
    val successCount = new AtomicInteger(0)
    val slaService = stub[SlaService]

    (slaService.getSlaByToken _).when(*).onCall { token: String =>
      Future successful SLA(token + "_user", rps)
    }

    val sut = new ActorBasedThrottlingServiceImpl(rps, slaService)

    val tokens = Gen.listOfN(tokensCount, Gen.identifier).sample.get

    val f = Future { //`global` is used so Await handled properly
      var i = 0
      var token = ""
      while (true) {
        token = if (i < tokens.size) {i += 1; tokens(i - 1)} else {i = 0; tokens(i)}
        if (Await.result(sut.isRequestAllowed(Some(token)), 1 hours))
          successCount.getAndIncrement()
      }
    }

    try {
      Await.ready(f, time)
    } catch {
      case _: TimeoutException =>
        val result =
          s"""
             | ================== SETUP ===================
             |
             | N = $tokensCount
             | K = $rps
             | T = $time
             |
             | =============== TEST RESULTS ===============
             |
             | expected successes (N*K*T) = ${tokensCount * rps * time.toSeconds}
             |       total actual success = ${successCount.get} *
             |
             | *
             |  . up to ${rps + rps/10} successes might be contributed by unauthorized behavior
             |  . please run this test few times as concrete result may be kid of fluctuation
             |
             |
             | """.stripMargin
        println(result)
    }
  }

}
