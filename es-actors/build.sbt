import Settings._
import org.scoverage.coveralls.Imports.CoverallsKeys._
import sbt._

lazy val root = project.root
  .setName("ES Actors")
  .setDescription("ES Actors")
  .setInitialCommand("_")
  .configureRoot
  .aggregate(common, client, server)

lazy val common = project.from("common")
  .setName("common")
  .setDescription("Common utilities")
  .setInitialCommand("_")
  .configureModule

lazy val client = project.from("client")
  .setName("client")
  .setDescription("Client project")
  .setInitialCommand("_")
  .configureModule
  .configureIntegrationTests
  .configureFunctionalTests
  .configureUnitTests
  .dependsOnProjects(common)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    mainClass in(Compile, run) := Some("com.broilogabriel.Client"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := Settings.defaultOrg,
    libraryDependencies += "org.elasticsearch" % "elasticsearch" % "2.4.1"
  )

lazy val server = project.from("server")
  .setName("server")
  .setDescription("Server project")
  .setInitialCommand("_")
  .configureModule
  .configureIntegrationTests
  .configureFunctionalTests
  .configureUnitTests
  .dependsOnProjects(common)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    mainClass in(Compile, run) := Some("com.broilogabriel.Server"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := Settings.defaultOrg,
    libraryDependencies += "org.elasticsearch" % "elasticsearch" % "2.4.1"
  )

coverallsToken := Some("IbJNNZ6lxeH9qiCC5O5at5W8eEE5B4LYL")
gitRepoDir:= ".."


