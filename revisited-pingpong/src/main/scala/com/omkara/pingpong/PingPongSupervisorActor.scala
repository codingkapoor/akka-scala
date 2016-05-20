package com.omkara.pingpong

import akka.actor.{ Actor, ActorRef, ActorLogging, Props}

class PingPongSupervisorActor(val routerActor: ActorRef) extends Actor with ActorLogging {
  import PingPongSupervisorActor._
  import PingPongActor._

  def receive = {

    case StartPingPongActors(numberOfActorsToSpawn) =>

      log.info("PingPongSupervisorActor is instantiating {} PingPongActors.", numberOfActorsToSpawn)
      for (i <- 1 to numberOfActorsToSpawn)
        context.actorOf(pingPongActorProps) ! SelfDiscover(routerActor)

  }
}

object PingPongSupervisorActor {

  def pingPongSupervisorActorProps(routerActor: ActorRef) =
    Props(classOf[PingPongSupervisorActor], routerActor)

  case class StartPingPongActors(numberOfActorsToSpawn: Int)

}
