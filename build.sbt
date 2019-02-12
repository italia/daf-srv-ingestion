import Versions._
import Environment._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

organization in ThisBuild := "it.gov.daf"
name := "daf-srv-injestion"


Seq(gitStampSettings: _*)

scalaVersion in ThisBuild := "2.11.11"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DockerPlugin)


libraryDependencies ++= Seq(
  ws,
  "it.gov.daf" %% "common" % Versions.dafCommonVersion
  //"io.swagger" %% "swagger-play2" % "1.5.1", already on common
  //"org.apache.spark" %% "spark-core" % "2.2.0",
  //"org.apache.spark" %% "spark-sql" % "2.2.0"
)

lazy val circe = "io.circe"

val circeDependencies = Seq(
  "circe-core",
  "circe-generic-extras",
  "circe-parser"
) map(circe %% _ % circeVersion)

libraryDependencies ++= circeDependencies
libraryDependencies += "play-circe" %% "play-circe" % "2.5-0.8.0"

resolvers ++= Seq(
  Resolver.mavenLocal,
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  //  "jeffmay" at "https://dl.bintray.com/jeffmay/maven",
  //  Resolver.url("sbt-plugins", url("http://dl.bintray.com/gruggiero/sbt-plugins"))(Resolver.ivyStylePatterns),
  //"cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  //"lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven",
  "daf repo" at s"$nexusUrl/maven-public/",
  "Bintary JCenter" at "http://jcenter.bintray.com"
)


// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
