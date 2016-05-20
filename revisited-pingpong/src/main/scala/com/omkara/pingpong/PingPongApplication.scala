package com.omkara.pingpong

import akka.actor.ActorSystem

object PingPongApplication extends App {
  import MasterActor._

  val system = ActorSystem("PingPongActorSystem")
  val master = system.actorOf(masterActorProps , "masterActor")
  
  master ! InitializeSys
  
  Thread sleep 5000
  
  master ! TerminateSys
  
  system.awaitTermination()

}