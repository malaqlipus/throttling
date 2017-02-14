package com.assignment.throttling.services

import scala.concurrent.Future

/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 14 Jan 2017
  *
  */
trait ThrottlingService {

  val graceRps:Int

  val slaService: SlaService

  def isRequestAllowed(token:Option[String]): Future[Boolean]
}