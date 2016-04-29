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

        case Some(UnreachableActorException(unreachable)) => {
          currentActorWithPingRole ! UnreachableActorException(unreachable)

          exceptionCaught = None
        }

        case Some(UnregisteredActorException(unregistered)) => {
          currentActorWithPingRole ! UnregisteredActorException(unregistered)
          currentActorWithPingRole ! PongMessage(pongMessage, messageIdentifier)

          exceptionCaught = None
        }

        case None => {
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