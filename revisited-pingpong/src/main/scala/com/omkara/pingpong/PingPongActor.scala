package com.omkara.pingpong

import akka.actor.{ ActorRef, ActorLogging, FSM, Props }
import scala.util.Random

class PingPongActor extends FSM[PingPongActor.State, PingPongActor.Data] with ActorLogging {
  import PingPongActor._
  import RouterActor._

  var router: ActorRef = _
  val messageIdentifier = Random

  val pingMessage = "ping"
  val pongMessage = "pong"
  
  startWith(Inactive, EmptyData)

  when(Pinging) {
    
    case Event(PongMessage(message, identifier), _) => {
      log.info("{} message received with identifier {}", message, identifier)
      
      sender ! PingMessage(pingMessage, messageIdentifier.nextInt())
      stay
    }

    case Event(UnregisteredActorException(unregistered), _) => {
      log.info("Caught UnregisteredActorException: {}", unregistered)
      stay
    }

    case Event(UnreachableActorException(unreachable), _) => {
      log.info("Caught UnreachableActorException: {}", unreachable)
      
      router ! ResetRoles
      stay
    }
    
  }

  when(Ponging) {
    
    case Event(PingMessage(message, identifier), _) => {
      log.info("{} message received with identifier {}", message, identifier)
      
      Thread sleep 100
      sender ! PongMessage(pongMessage, identifier)
      stay
    }
    
  }

  when(Inactive) {
    
    case Event(SelfDiscover(routerActor), _) => {
      router = routerActor
      
      log.info("Registering with the RouterActor")
      router ! Register
      
      stay
    }
    
  }

  whenUnhandled {
    
    case Event(PingNow, _) => {
      log.info("Moving to the pinging state.")
      
      router ! PingMessage(pingMessage, messageIdentifier.nextInt())
      goto(Pinging) using EmptyData
    }

    case Event(PongNow, _) => {
      log.info("Moving to the ponging state.")
      goto(Ponging) using EmptyData
    }

    case Event(Enough, _) => {
      log.info("Unregistering with the RouterActor and moving to the Inactive state.")
      router ! Unregister
      goto(Inactive) using EmptyData
    }

  }

}

object PingPongActor {

  val pingPongActorProps = Props[PingPongActor]

  sealed trait State
  case object Pinging extends State
  case object Ponging extends State
  case object Inactive extends State

  sealed trait Data
  case object EmptyData extends Data

  case class PingMessage(message: String, messageIdentifier: Int)
  case class PongMessage(message: String, messageIdentifier: Int)

  case class SelfDiscover(routerActor: ActorRef)

  case object PingNow
  case object PongNow
  case object Enough

  sealed trait Exception
  case class UnregisteredActorException(unregistered: Set[ActorRef]) extends Exception
  case class UnreachableActorException(unreachable: Set[ActorRef]) extends Exception

}