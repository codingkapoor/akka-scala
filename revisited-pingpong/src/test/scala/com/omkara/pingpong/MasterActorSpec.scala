package com.omkara.pingpong

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.testkit.{ TestKit, TestActorRef, ImplicitSender }
import org.scalatest.{ FlatSpecLike, Matchers, BeforeAndAfterAll }

class MasterActorSpec extends TestKit(ActorSystem("MasterActorSpec", ConfigFactory.load()))
    with ImplicitSender with FlatSpecLike with Matchers with BeforeAndAfterAll {
  import MasterActor._
  import RouterActor._
  import PingPongSupervisorActor._

  trait Master {
    val masterRef = TestActorRef[MasterActor]
    val master = masterRef.underlyingActor
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "MasterActor" should "create & ask PingPongSupervisorActor to start PingPongActor actors" in new Master {
      masterRef ! InitializeSys
      expectNoMsg
    }
  
  it should "create & ask RouterActor to assign roles to PingPongActor actors" in new Master {
    masterRef ! InitializeSys
    expectNoMsg
  }

  it should "ask RouterActor to conclude as part of actor system termination" in new Master {
    // MasterActor must always have the 'router' instance assigned before it could receive Terminate message
    master.router = testActor

    masterRef ! TerminateSys
    expectMsg(Conclude)
  }

}