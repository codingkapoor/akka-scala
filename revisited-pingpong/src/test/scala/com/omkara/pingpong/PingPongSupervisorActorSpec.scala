package com.omkara.pingpong

import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import akka.actor.{ ActorSystem, ActorRef }
import com.typesafe.config.ConfigFactory
import akka.actor.Props

class PingPongSupervisorActorSpec extends TestKit(ActorSystem("PingPongSupervisorSpec", ConfigFactory.load()))
    with ImplicitSender with FlatSpecLike with Matchers with BeforeAndAfterAll {
  import PingPongSupervisorActor._
  import RouterActor._

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  trait Supervisor {
    val router = system.actorOf(routerActorProps)
    val supervisorRef = TestActorRef[PingPongSupervisorActor](Props(classOf[PingPongSupervisorActor], router))
    val supervisor = supervisorRef.underlyingActor
  }

  "PingPongSupervisorActorSpec" should "have router actor passed to it upon initialization" in new Supervisor {
    supervisor.routerActor shouldBe a[ActorRef]
  }

  it should "spawn PingPongActor actors & ask them to self discover with the RouterActor" in new Supervisor {
    supervisorRef ! StartPingPongActors(1)
    expectNoMsg
  }
}
