package com.omkara.pingpong

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }

class MasterActor extends Actor with ActorLogging {
  import MasterActor._
  import PingPongActor._
  import RouterActor._
  import PingPongSupervisorActor._

  var router: ActorRef = _
  var supervisor: ActorRef = _

  def receive = {

    case InitializeSys => {
      log.info("MasterActor is instantiating a RouterActor instance.")
      router = context.actorOf(routerActorProps, "router-actor")
      
      log.info("MasterActor is instantiating PingPongSupervisorActor.")
      supervisor = context.actorOf(pingPongSupervisorActorProps(router), "pingpong-supervisor-actor")
      supervisor ! StartPingPongActors(5)
      
      Thread sleep 1000
      
      log.info("MasterActor has asked RouterActor to assign roles.")
      router ! AssignRoles
    }

    case InitiateSysTermination => {
      log.info("MasterActor is initiating actor system termination.")
      router ! TerminateSys
    }
  }
}

object MasterActor {

  val masterActorProps = Props[MasterActor]

  case object InitializeSys
  case object InitiateSysTermination

}