package com.omkara.pingpong

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import akka.testkit.{ TestKit, TestActorRef, ImplicitSender }
import org.scalatest.{ FlatSpecLike, Matchers, BeforeAndAfterAll }

class MasterActorSpec extends TestKit(ActorSystem("MasterActorSpec", ConfigFactory.load()))
    with ImplicitSender with FlatSpecLike with Matchers with BeforeAndAfterAll {
  import MasterActor._
  import RouterActor._

  trait Master {
    val masterRef = TestActorRef[MasterActor]
    val master = masterRef.underlyingActor
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "MasterActor" should "create a RouterActor instance " +
    "and provided number of PingPongActor instances" in new Master {
      masterRef ! Initialize(2)
      expectNoMsg
    }

  it should "initiate actor system termination" in new Master {
    // MasterActor must always have the 'router' instance assigned before it could receive Terminate message
    master.router = testActor

    masterRef ! InitiateSysTermination
    expectMsg(TerminateSys)
  }

}