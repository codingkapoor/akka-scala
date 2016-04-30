package com.omkara.pingpong

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import scala.util.Random

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RouterActor extends Actor with ActorLogging {
  import RouterActor._
  import PingPongActor._

  var routees: Set[ActorRef] = Set[ActorRef]()

  var currentActorWithPingRole: ActorRef = _

  var pongMessage: String = _
  var messageIdentifier: Int = _

  var startAwaitingReplies: Long = _

  var repliesReceivedFrom: Set[ActorRef] = Set[ActorRef]()

  var exceptionCaught: Option[Exception] = None

  var unreachableMap = scala.collection.mutable.Map[ActorRef, Int]()

  def receive = {

    case Register => {
      routees += sender
      log.info("{} has been registered.", sender.path.name)

    }

    case Unregister => {
      routees -= sender
      log.info("{} has been unregistered.", sender.path.name)
    }

    case PingMessage(message, identifier) => {
      log.info("RouterActor has received a {} message with identifier {} from the PingPongActor {}", message, identifier, sender.path.name)

      messageIdentifier = identifier

      if (routees.contains(sender)) {
        (routees - sender).foreach { x => { x ! PingMessage(message, identifier) } }
        repliesReceivedFrom = Set[ActorRef]()

        // the duration must accommodate the time taken by actors to respond and time taken by the router to process it's mailbox
        // i.e., actors taking unusually long duration must be considered unresponsive
        log.info("Starting the stop watch.")
        var stopWatch = Future {
          var start = System.currentTimeMillis()
          while (start + 100 >= System.currentTimeMillis()) {}
        }

        stopWatch onSuccess {
          case result => {
            self ! StopWatchEnded
          }
        }

      }

    }

    case PongMessage(message, identifier) => {
      log.info("RouterActor has received a {} message with identifier {} from the PingPongActor {}", message, identifier, sender.path.name)

      if (identifier.==(messageIdentifier)) {
        repliesReceivedFrom += sender
        pongMessage = message
      }

    }

    case StopWatchEnded => {
      log.info("Stop watch has ended.")
      var unreachable = (routees - currentActorWithPingRole) diff repliesReceivedFrom
      if (!unreachable.isEmpty) {
        exceptionCaught = Some(UnreachableActorException(unreachable))
      }

      var unregistered = repliesReceivedFrom diff (routees - currentActorWithPingRole)
      if (!unregistered.isEmpty) {
        exceptionCaught = Some(UnregisteredActorException(unregistered))
      }

      exceptionCaught match {

        // there is a possibility that both unreachable and unregistered have values
        // however if UnreachableActorException is caught roles are getting reset anyway hence replying pining actor
        // with UnregisteredActorException would be insignificant
        case Some(UnreachableActorException(unreachable)) => {

          updateUnreachableMap(unreachable)

          currentActorWithPingRole ! UnreachableActorException(unreachable)

          exceptionCaught = None
        }

        case Some(UnregisteredActorException(unregistered)) => {
          // content in unreachable map is insignificant outside UnreachableActorException handling
          clearUnreachableMap

          currentActorWithPingRole ! UnregisteredActorException(unregistered)
          currentActorWithPingRole ! PongMessage(pongMessage, messageIdentifier)

          exceptionCaught = None
        }

        case None => {
          clearUnreachableMap

          currentActorWithPingRole ! PongMessage(pongMessage, messageIdentifier)
        }
      }
    }

    case AssignRoles => {

      log.info("RouterActor is assigning roles.")

      currentActorWithPingRole = Random shuffle (routees) head

      currentActorWithPingRole ! PingNow

      (routees - currentActorWithPingRole).foreach { _ ! PongNow }

    }

    case ResetRoles => {

      log.info("RouterActor is resetting roles.")

      unregisterActorsUnreachable3TimesInARow

      currentActorWithPingRole ! PongNow

      currentActorWithPingRole = Random shuffle (routees - currentActorWithPingRole) head

      currentActorWithPingRole ! PingNow

    }

    case Terminate => {

      log.info("RouterActor is terminating actor system gracefully.")

      routees.foreach { _ ! Enough }

      Thread sleep 1000
      context.system.shutdown()

    }

  }

  def updateUnreachableMap(unreachable: Set[ActorRef]) = {
    // actors present in map but absent unreachable set must be removed from the map
    // since we need to maintain map of only those actors that aren't reachable for three consecutive times
    (unreachableMap.keySet diff unreachable).foreach { actor => unreachableMap -= actor }

    // unreachable map is maintains (k,v) as (unreachable actor, number of consecutive times it has been found unreachable)
    unreachable.foreach {
      actor =>
        if (!unreachableMap.contains(actor)) unreachableMap += (actor -> 1) else
          unreachableMap(actor) += 1
    }
  }

  def unregisterActorsUnreachable3TimesInARow = {
    unreachableMap.foreach { actor => if (actor._2 >= 3) routees -= actor._1 }
  }

  def clearUnreachableMap = {
    if (!unreachableMap.isEmpty) unreachableMap.clear
  }

}

object RouterActor {
  val routerActorProps = Props[RouterActor]

  case object Register
  case object Unregister

  case object AssignRoles
  case object ResetRoles

  case object StopWatchEnded

  case object Terminate
}