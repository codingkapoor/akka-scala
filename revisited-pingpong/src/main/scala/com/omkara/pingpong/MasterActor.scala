package com.omkara.pingpong

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }

class MasterActor extends Actor with ActorLogging {
  import MasterActor._
  import PingPongActor._
  import RouterActor._

  var router: ActorRef = _

  def receive = {

    case Initialize(numberOfActorsToSpawn) => {
      log.info("MasterActor is instantiating a RouterActor instance.")
      router = context.actorOf(routerActorProps, "router-actor")
      
      log.info("MasterActor is instantiating {} PingPongActors.", numberOfActorsToSpawn)
      for (i <- 1 to numberOfActorsToSpawn)
        context.actorOf(pingPongActorProps) ! SelfDiscover(router)
        
      Thread sleep 1000
      
      log.info("MasterActor is asking RouterActor to assign roles.")
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

  case class Initialize(numberOfActorsToSpawn: Int)
  case object InitiateSysTermination

}