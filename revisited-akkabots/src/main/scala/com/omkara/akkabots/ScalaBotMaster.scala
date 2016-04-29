package com.omkara.akkabots

import akka.actor.Actor
import akka.actor.Props
import scala.util.Random

class ScalaBotMaster extends Actor {
  import ScalaBotMaster._
  import ScalaBot._

  val listOfDirections = List(FORWARD, BACKWARD, LEFT, RIGHT)

  for (indx <- 1 to 3) {
    context.actorOf(botProps)
  }

  def receive = {
    case StartChildBots => {
      println("Master has started children bots with default FORWARD direction.")
      context.children.foreach {
        child =>
          {
            child ! Move(FORWARD)
            child ! GetRobotState
          }
      }
    }

    case ChangeDirections => {
      println("\nMaster has reassigned random directions.")
      context.children.foreach {
        child =>
          {
            child ! Move(Random.shuffle(listOfDirections).head)
            child ! GetRobotState
          }
      }
    }

    case StopChildBots =>
      println("\nMaster has stopped children bots.")
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
      if (moving) println(s"${sender.path.name} is moving $direction.")
      else println(s"${sender.path.name} has stopped.")
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