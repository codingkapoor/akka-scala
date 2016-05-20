package com.omkara.pingpong

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.actor.{ AllForOneStrategy, ActorKilledException, ActorInitializationException }
import akka.actor.SupervisorStrategy._

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

      log.info("MasterActor is asking RouterActor to assign roles.")
      router ! AssignRoles
    }

    case TerminateSys => {
      log.info("MasterActor is initiating actor system termination.")
      router ! Conclude

      Thread sleep 1000

      log.info("MasterActor is terminating actor system gracefully.")
      context.system.shutdown()

    }
  }

  override val supervisorStrategy = AllForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException         => Stop
    case _: Exception                    => Restart
  }

}

object MasterActor {

  val masterActorProps = Props[MasterActor]

  case object InitializeSys
  case object TerminateSys

}