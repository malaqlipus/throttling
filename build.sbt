name := "throttling-assignment"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.3" withSources(),
  "com.typesafe.akka" %% "akka-actor" % "2.4.17" withSources(),

  "com.typesafe.akka" %% "akka-testkit" % "2.4.17" % "test" withSources(),
  "org.scalacheck"    %% "scalacheck" % "1.13.4" % "test" withSources(),
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.3" % "test" withSources(),
  "org.scalatest" %% "scalatest" % "3.0.0" % "test" withSources(),
  "org.scalamock" %% "scalamock-scalatest-support" % "3.4.1" % "test" withSources()

)


//TODO: make me nice