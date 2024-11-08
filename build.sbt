version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
//  .enablePlugins(JacocoItPlugin)
  .settings(
    name := """tube-lytics-java""",
    organization := "com.example",
    crossScalaVersions := Seq("2.13.14", "3.3.3"),
    scalaVersion := crossScalaVersions.value.head,
    (Test / javaOptions) += "-Dtestserver.port=19001",
    (Test / testOptions) := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v")),
    javacOptions ++= Seq("-source", "11", "-target", "11")
  )
  //.enablePlugins(PlayNettyServer).disablePlugins(PlayPekkoHttpServer) // uncomment to use the Netty backend

libraryDependencies ++= Seq(
  guice,
  ws,
  "org.mockito" % "mockito-core" % "5.11.0" % "test",
  "org.apache.pekko" %% "pekko-testkit" % "1.0.3" % Test,
  "junit" % "junit" % "4.13.2" % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test,
  "org.awaitility" % "awaitility" % "4.2.2" % Test,
)

