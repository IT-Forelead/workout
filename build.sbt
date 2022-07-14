import Dependencies._

lazy val projectSettings = Seq(
  version      := "1.0",
  scalaVersion := "2.13.8",
  organization := "IT-Forelead"
)

lazy val root = project
  .in(file("."))
  .settings(projectSettings: _*)
  .settings(
    name := "workout"
  )
  .aggregate(server, tests)

lazy val server = project
  .in(file("modules/server"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(projectSettings: _*)
  .settings(
    name              := "workout",
    scalafmtOnCompile := true,
    libraryDependencies ++= coreLibraries,
    scalacOptions ++= CompilerOptions.cOptions,
    Test / compile / coverageEnabled    := true,
    Compile / compile / coverageEnabled := false
  )
  .settings(
    Docker / packageName := "workout",
    dockerBaseImage      := "openjdk:11-jre-slim-buster",
    dockerUpdateLatest   := true
  )

lazy val tests = project
  .in(file("modules/tests"))
  .configs(IntegrationTest)
  .settings(projectSettings: _*)
  .settings(
    name := "workout-test-suite",
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Defaults.itSettings,
    scalacOptions ++= CompilerOptions.cOptions,
    IntegrationTest / parallelExecution  := false,
    IntegrationTest / testForkedParallel := false,
    libraryDependencies ++= testLibraries,
    scalacOptions ++= CompilerOptions.cOptions
  )
  .dependsOn(server)

val runItTests = inputKey[Unit]("Runs It tests")
val runTests   = inputKey[Unit]("Runs tests")
val runServer  = inputKey[Unit]("Runs server")

runServer := {
  (server / Compile / run).evaluated
}

runTests := {
  (tests / Test / test).value
}

runItTests := {
  (tests / IntegrationTest / test).value
}

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)
Global / onLoad := (Global / onLoad).value.andThen(state => "project server" :: state)
