package com.assignment.throttling.services

import scala.concurrent.Future

/**
  *
  * @author Andriy Salivonov, andriy.salivonov@gmail.com
  * @since 14 Jan 2017
  *
  */
trait SlaService {

  def getSlaByToken(token:String):Future[SLA]
}

case class SLA(user:String, rps:Int)
