package com.omkara.akkabots

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestActorRef, ImplicitSender }
import org.scalatest.{ Matchers, FlatSpecLike, BeforeAndAfterAll }

class ScalaBotSpec extends TestKit(ActorSystem("ScalaBotSpec", ConfigFactory.load()))
    with ImplicitSender with FlatSpecLike with Matchers with BeforeAndAfterAll {
  import ScalaBot._

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  trait ScalaBotTest {
    val scalaBotRef = TestActorRef[ScalaBot]
    val scalaBot = scalaBotRef.underlyingActor
  }

  "ScalaBot" should "start moving in the instructed direction upon reception of 'Move' message" in new ScalaBotTest {

    scalaBotRef ! Move(LEFT)

    scalaBot.moving shouldEqual true
    scalaBot.direction shouldEqual LEFT
  }

  it should "stop moving upon reception of 'Stop' message" in new ScalaBotTest {

    scalaBotRef ! Stop

    scalaBot.moving shouldEqual false
  }

  it should "report the sender with current 'RobotState' upon reception of 'GetRobotState' message" in new ScalaBotTest {
    scalaBot.moving = true
    scalaBot.direction = LEFT

    scalaBotRef ! GetRobotState

    expectMsg(RobotState(LEFT, true))
  }

}