package com.omkara.pingpong

import akka.actor.ActorSystem

object PingPongApplication extends App {
  import MasterActor._

  val system = ActorSystem("PingPongActorSystem")
  val master = system.actorOf(masterActorProps , "masterActor")
  
  master ! Initialize(5)
  
  Thread sleep 5000
  
  master ! InitiateSysTermination
  
  system.awaitTermination()

}