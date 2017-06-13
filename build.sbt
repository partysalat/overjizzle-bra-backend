name := "2null16-bra"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.xerial" % "sqlite-jdbc" % "3.16.1",
  "com.brsanthu" % "migbase64" % "2.2",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.0-akka-2.4.x"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.2"
)