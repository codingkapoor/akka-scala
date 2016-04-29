import sbt._
import Keys._

object ExploreAkkaBuild extends Build {
    lazy val commonSettings = 
	Seq (
	    scalaVersion := "2.11.8",
	    libraryDependencies ++= 
		Seq(
		    "com.typesafe.akka" %% "akka-actor" % "2.4.4",
		    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
		    "com.typesafe.akka" %% "akka-testkit" % "2.4.4" % "test"
		)
	)

    lazy val explore_akka = 
	Project(id = "explore-akka", base = file(".")) aggregate(revisited_akkabots, revisited_pingpong) settings(commonSettings: _*)

    lazy val revisited_akkabots = 
	Project(id = "revisited-akkabots", base = file("revisited-akkabots")) settings(commonSettings: _*)

    lazy val revisited_pingpong = 
	Project(id = "revisited-pingpong", base = file("revisited-pingpong")) settings(commonSettings: _*)
}
