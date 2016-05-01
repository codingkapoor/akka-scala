package com.omkara.akkabots

import akka.actor.{ Actor, ActorLogging }
import akka.actor.Props
import scala.util.Random

class ScalaBotMaster extends Actor with ActorLogging {
  import ScalaBotMaster._
  import ScalaBot._

  val listOfDirections = List(FORWARD, BACKWARD, LEFT, RIGHT)

  for (indx <- 1 to 3) {
    context.actorOf(akkabotProps)
  }

  def receive = {
    case StartChildBots => {
      log.info("Master has started children bots with default FORWARD direction.")
      context.children.foreach {
        child =>
          {
            child ! Move(FORWARD)
            child ! GetRobotState
          }
      }
    }

    case ChangeDirections => {
      log.info("Master has reassigned random directions.")
      context.children.foreach {
        child =>
          {
            child ! Move(Random.shuffle(listOfDirections).head)
            child ! GetRobotState
          }
      }
    }

    case StopChildBots =>
      log.info("Master has stopped children bots.")
      context.children.foreach {
        child =>
          {
            child ! Stop
            child ! GetRobotState
          }
      }

    case Terminate =>
      context.system.shutdown()

    case RobotState(direction: Direction, moving: Boolean) => {
      if (moving) log.info(s"${sender.path.name} is moving $direction.")
      else log.info(s"${sender.path.name} has stopped.")
    }
  }

}

object ScalaBotMaster {
  val masterProps = Props[ScalaBotMaster]

  case object StartChildBots
  case object ChangeDirections
  case object StopChildBots
  case object Terminate
}