package com.assignment.throttling.directives

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.assignment.throttling.services.impl.ActorBasedThrottlingServiceImpl
import com.assignment.throttling.services.{SLA, SlaService}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

import scala.concurrent.Future
import scala.language.postfixOps

/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 14 Feb 2017
  *
  */
class ThrottlingDirectiveTest extends FlatSpec with MockFactory with ScalatestRouteTest with Matchers with OneInstancePerTest {

  behavior of "Throttling directive"

  val requestsCount = 100
  val rps = 1000
  val token = "099q984752135"

  it should "... pass performance test" in {
    val slaService = stub[SlaService]

    (slaService.getSlaByToken _).when(*).onCall { token: String =>
      Future successful SLA(token + "_user", rps)
    }

    val service = new ActorBasedThrottlingServiceImpl(rps, slaService)
    val t = new ThrottlingDirective(service)

    val testRoute1 = routeWithoutThrottling(token)

    //warming up for runtime optimization
    warmUp(testRoute1)


    val withoutTt = time {
      (1 to requestsCount).foreach { _ =>
        Get() ~> testRoute1 ~> check {
          responseAs[String] shouldEqual "Captain on the bridge!"
        }
      }
    }

    val testRoute2 = routeWithThrottling(t)(token)

    val withTt = time {
      try {
        (1 to requestsCount).foreach { _ =>
          Get() ~> testRoute2 ~> check {
            responseAs[String] shouldEqual "Captain on the bridge!"
          }
        }
      } catch {
        case _ : Throwable => // ignore
      }
    }
    val result =
      s"""
         |
         | =============== TEST RESULTS ===============
         |
         | . if SLA rps is exceeded: throttling is always faster
         |
         | . if SLA rps is NOT exceeded:
         |
         | requests count: $requestsCount
         |
         |         without throttling: $withoutTt milliseconds
         |            with throttling: $withTt milliseconds
         |
         |         overall difference: ${withTt - withoutTt} milliseconds
         | avg per request difference: ${"%.2f".format((withTt - withoutTt).toDouble / requestsCount )} milliseconds *
         |
         | *
         |   please run this test few times as concrete result may be kid of fluctuation
         |
         | """.stripMargin
    println(result)
  }

  private def warmUp(testRoute1: Route) = {
    (1 to 5000).foreach { _ =>
      Get() ~> testRoute1 ~> check {
        responseAs[String] shouldEqual "Captain on the bridge!"
      }
    }
  }

  private def routeWithThrottling(throttlingDirective: ThrottlingDirective)(token: String) =
    get {
      pathSingleSlash {
        throttlingDirective.throttling(Some(token)) {
          complete {
            "Captain on the bridge!"
          }
        }
      }
    }

  private def routeWithoutThrottling(token: String) =
    get {
      pathSingleSlash {
        complete {
          "Captain on the bridge!"
        }
      }
    }

  private def time[T](thunk: => T): Long = {
    val t1 = System.currentTimeMillis
    thunk
    val t2 = System.currentTimeMillis
    t2 - t1
  }

}
