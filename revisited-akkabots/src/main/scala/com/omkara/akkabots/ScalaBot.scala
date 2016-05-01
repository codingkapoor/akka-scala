package com.omkara.akkabots

import akka.actor.Actor
import akka.actor.Props

class ScalaBot extends Actor {
  import ScalaBot._

  var moving: Boolean = false
  var direction: Direction = FORWARD

  def receive = {
    case Move(newDirection) => {
      moving = true
      direction = newDirection
    }

    case Stop => {
      moving = false
    }

    case GetRobotState =>
      sender ! RobotState(direction, moving)

  }
}

object ScalaBot {
  val akkabotProps = Props[ScalaBot]

  sealed abstract class Direction

  case object FORWARD extends Direction
  case object BACKWARD extends Direction
  case object LEFT extends Direction
  case object RIGHT extends Direction

  case class Move(direction: Direction)
  case object Stop

  case class RobotState(direction: Direction, moving: Boolean)
  case object GetRobotState
}

