package com.assignment.throttling.directives

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Rejection}
import com.assignment.throttling.services.ThrottlingService

/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 14 Jan 2017
  *
  */
class ThrottlingDirective(private val throttlingService: ThrottlingService) {

  def throttling(token: Option[String]): Directive0 = {
    onSuccess(throttlingService.isRequestAllowed(token)) flatMap { res =>
      if (res)
        pass
      else
        reject(ThrottlingRejection)
    }
  }
}

//TODO: add ExceptionHandler which will handle also !timeout exceptions!

//TODO: move me
object ThrottlingRejection extends Rejection