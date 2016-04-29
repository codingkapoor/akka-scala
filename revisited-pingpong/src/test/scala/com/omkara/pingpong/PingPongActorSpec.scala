package com.omkara.pingpong

import scala.AnyVal
import scala.util.Random

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem

import akka.testkit.{ ImplicitSender, TestFSMRef, TestKit }
import org.scalatest.{ BeforeAndAfterAll, Matchers, FlatSpecLike }

class PingPongActorSpec extends TestKit(ActorSystem("PingPongActorSpec",
  ConfigFactory.parseString("""
      akka.loggers = ["akka.testkit.TestEventListener"]
      akka.stdout-loglevel = "OFF"
      akka.loglevel = "OFF"
    """)))
    with ImplicitSender
    with FlatSpecLike
    with Matchers
    with BeforeAndAfterAll {
  import PingPongActor._
  import RouterActor._

  trait Messages {
    val pingMessage = "ping"
    val pongMessage = "pong"

    val identifier = Random.nextInt()
  }

  trait Fsm extends Messages {
    val fsmRef = TestFSMRef(new PingPongActor)
    val fsm = fsmRef.underlyingActor
  }

  trait PingingFsm extends Fsm {
    // PingPongActor must always have the 'router' instance assigned while being in Pinging state
    fsm.router = testActor
    fsmRef.setState(Pinging, EmptyData)
  }

  trait PongingFsm extends Fsm {
    // PingPongActor must always have the 'router' instance assigned while being in Ponging state
    fsm.router = testActor
    fsmRef.setState(Ponging, EmptyData)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A PingPongActor upon initialization" should "be in Inactive state :: " +
    "(Inactive, EmptyData)" in new Fsm {

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Inactive

    }

  it should "ask RouterActor to register self :: " +
    "{SelfDiscover}(Inactive, EmptyData) -> {Register}(Inactive, EmptyData)" in new Fsm {

      fsmRef ! SelfDiscover(testActor)
      expectMsg(Register)

      fsm.router shouldEqual testActor

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Inactive

    }

  it should "move to Pinging state with a PingMessage reply:: " +
    "{PingNow}(Inactive, EmptyData) -> {PingMessage}(Pinging, EmptyData)" in new Fsm {

      // PingPongActor must have the 'router' instance assigned after self discovery
      fsm.router = testActor

      fsmRef ! PingNow
      expectMsgPF() {
        case PingMessage(pingMessage, _) =>
      }

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Pinging

    }

  it should "move to Ponging state without any replies :: " +
    "{PongNow}(Inactive, EmptyData) -> {}(Ponging, EmptyData)" in new Fsm {

      fsmRef ! PongNow
      expectNoMsg

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Ponging

    }

  "A PingPongActor while in Pinging state" should "stays in Pinging state with a PingMessage reply ::" +
    "{PingNow}(Pinging, EmptyData) -> {PingMessage}(Pinging, EmptyData)" in new PingingFsm {

      fsmRef ! PingNow
      expectMsgPF() {
        case PingMessage(pingMessage, _) =>
      }

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Pinging

    }

  it should "move to Ponging state without any replies:: " +
    "{PongNow}(Pinging, EmptyData) -> {}(Ponging, EmptyData)" in new PingingFsm {

      fsmRef ! PongNow
      expectNoMsg

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Ponging

    }

  it should "reply with the PingMessage :: " +
    "{PongMessage}(Pinging, EmptyData) -> {PingMessage}(Pinging, EmptyData)" in new PingingFsm {

      fsmRef ! PongMessage(pongMessage, identifier)
      expectMsgPF() {
        case PingMessage(pingMessage, _) =>
      }

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Pinging

    }

  it should "not respond, when caught UnregisteredActorException :: " +
    "{UnregisteredActorException}(Pinging, EmptyData) -> {}(Pinging, EmptyData)" in new PingingFsm {

      fsmRef ! UnregisteredActorException(Set())
      expectNoMsg

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Pinging

    }

  it should "ask RouterActor to reset roles, when caught UnreachableActorException :: " +
    "{UnreachableActorException}(Pinging, EmptyData) -> {ResetRoles}(Pinging, EmptyData)" in new PingingFsm {

      fsmRef ! UnreachableActorException(Set())
      expectMsg(ResetRoles)

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Pinging

    }

  it should "ask RouterActor to unregister the sender & move to Inactive state :: " +
    "{Enough}(Pinging, EmptyData) -> {Unregister}(Inactive, EmptyData)" in new PingingFsm {

      fsmRef ! Enough
      expectMsg(Unregister)

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Inactive

    }

  "A PingPongActor while in Ponging state" should "stay in Ponging state without any replies :: " +
    "{PongNow}(Ponging, EmptyData) -> {}(Ponging, EmptyData)" in new PongingFsm {

      fsmRef ! PongNow

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Ponging

    }

  it should "move to Pinging state with a PingMessage reply :: " +
    "{PingNow}(Ponging, EmptyData) -> {PingMessage}(Pinging, EmptyData)" in new PongingFsm {

      fsmRef ! PingNow
      expectMsgPF() {
        case PingMessage(pingMessage, _) =>
      }

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Pinging

    }

  it should "reply with a PongMessage :: " +
    "{PingMessage}(Ponging, EmptyData) -> {PongMessage}(Ponging, EmptyData)" in new PongingFsm {

      fsmRef ! PingMessage(pingMessage, identifier)
      expectMsgPF() {
        case PongMessage(pongMessage, _) =>
      }

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Ponging

    }

  it should "ask RouterActor to unregister the sender & move to Inactive state :: " +
    "{Enough}(Ponging, EmptyData) -> {Unregister}(Inactive, EmptyData)" in new PongingFsm {

      fsmRef ! Enough
      expectMsg(Unregister)

      fsmRef.stateData shouldEqual EmptyData
      fsmRef.stateName shouldEqual Inactive

    }

}