package com.omkara.akkabots

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestActorRef, ImplicitSender }
import org.scalatest.{ Matchers, FlatSpecLike, BeforeAndAfterAll }
import com.omkara.akkabots.ScalaBotMaster.StartChildBots
import com.omkara.akkabots.ScalaBotMaster.ChangeDirections
import com.omkara.akkabots.ScalaBot.RobotState

class ScalaBotMasterSpec extends TestKit(ActorSystem("ScalaBotSpec", ConfigFactory.load()))
    with ImplicitSender with FlatSpecLike with Matchers with BeforeAndAfterAll {
  import ScalaBotMaster._
  import ScalaBot._

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  trait ScalaBotMasterTest {
    val masterRef = TestActorRef[ScalaBotMaster]
    val master = masterRef.underlyingActor
  }

  "ScalaBotMaster" should "start 3 children bots with default FORWARD direction" in new ScalaBotMasterTest {

    master.context.children.size shouldEqual 3
    
    masterRef ! StartChildBots
    expectNoMsg
    
  }

  it should "reassign random directions to its children bots" in new ScalaBotMasterTest {

    masterRef ! ChangeDirections
    expectNoMsg

  }

  it should "stop children bots" in new ScalaBotMasterTest {

    masterRef ! StopChildBots
    expectNoMsg

  }

  it should "log received state of bots" in new ScalaBotMasterTest {

    masterRef ! RobotState(LEFT, true)
    expectNoMsg

  }
}