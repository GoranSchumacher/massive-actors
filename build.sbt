name := """Clustered chat"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

val akkaVersion = "2.4.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.webjars" %% "webjars-play" % "2.4.0",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars" % "handlebars" % "4.0.2",
  // Depends on: http://wkhtmltopdf.org/downloads.html
  "io.github.cloudify" %% "spdf" % "1.3.1",
  specs2 % Test,
  ws
)

val persistence = Seq(
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.github.scullxbones" %% "akka-persistence-mongo-casbah" % "1.1.10",
  "org.mongodb" %% "casbah" % "3.1.0"
)

libraryDependencies ++=persistence

libraryDependencies += "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.2.0"

libraryDependencies += "com.sksamuel.elastic4s" %% "elastic4s-jackson" % "2.2.0"


resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

LessKeys.compress in Assets := true

pipelineStages := Seq(digest)

includeFilter in (Assets, LessKeys.less) := "*.less"

javaOptions in Test ++= Seq("-Dlogger.resource=logback-test.xml")


fork in run := true