package com.omkara.akkabots

import akka.actor.ActorSystem
import akka.actor.Props

object ScalaBotMain extends App {
  import ScalaBotMaster._

  val system = ActorSystem()

  val master = system.actorOf(masterProps)

  master ! StartChildBots

  Thread sleep 1000

  master ! ChangeDirections

  Thread sleep 1000

  master ! StopChildBots

  Thread sleep 1000

  master ! Terminate
}
