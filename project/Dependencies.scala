import Dependencies.Libraries._
import sbt._

object Dependencies {
  object Versions {
    val awsSdk        = "1.12.111"
    val awsSoftwareS3 = "2.16.104"

    val cats          = "2.7.0"
    val catsEffect    = "3.3.11"
    val circe         = "0.14.1"
    val fs2           = "3.2.7"
    val http4s        = "0.23.11"
    val log4cats      = "2.3.0"
    val skunk         = "0.3.1"
    val logback       = "1.2.11"
    val ciris         = "2.3.2"
    val refined       = "0.9.29"
    val http4sJwtAuth = "1.0.0"
    val redis4cats    = "1.1.1"
    val newtype       = "0.4.4"
    val derevo        = "0.13.0"
    val monocle       = "3.1.0"
    val tsec          = "0.4.0"
    val squants       = "1.8.3"
    val catsRetry     = "3.1.0"
    val guava         = "31.0.1-jre"

    val weaver        = "0.7.11"
    val testContainer = "1.17.1"
    val postgresql    = "42.3.6"
  }

  object Libraries {
    def awsJdk(artifact: String): ModuleID = "com.amazonaws"          % artifact % Versions.awsSdk
    val awsCore                            = awsJdk("aws-java-sdk-core")
    val awsS3                              = awsJdk("aws-java-sdk-s3")
    val awsSoftwareS3                      = "software.amazon.awssdk" % "s3"     % Versions.awsSoftwareS3

    def circe(artifact: String): ModuleID = "io.circe" %% s"circe-$artifact" % Versions.circe

    def skunk(artifact: String): ModuleID = "org.tpolecat" %% artifact % Versions.skunk

    def ciris(artifact: String): ModuleID = "is.cir" %% artifact % Versions.ciris

    def http4s(artifact: String): ModuleID = "org.http4s" %% s"http4s-$artifact" % Versions.http4s

    def refined(artifact: String): ModuleID = "eu.timepit" %% artifact % Versions.refined

    def derevo(artifact: String): ModuleID = "tf.tofu" %% s"derevo-$artifact" % Versions.derevo

    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeRefined = circe("refined")

    val skunkCore    = skunk("skunk-core")
    val skunkCirce   = skunk("skunk-circe")
    val skunkRefined = skunk("refined")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val derevoCore  = derevo("core")
    val derevoCats  = derevo("cats")
    val derevoCirce = derevo("circe-magnolia")

    val http4sDsl    = http4s("dsl")
    val http4sServer = http4s("ember-server")
    val http4sClient = http4s("ember-client")
    val http4sCirce  = http4s("circe")

    val refinedType = refined("refined")
    val refinedCats = refined("refined-cats")
    val squants     = "org.typelevel" %% "squants" % Versions.squants

    val redis4catsEffects  = "dev.profunktor" %% "redis4cats-effects"  % Versions.redis4cats
    val redis4catsLog4cats = "dev.profunktor" %% "redis4cats-log4cats" % Versions.redis4cats
    val guava              = "com.google.guava"       % "guava"               % Versions.guava

    val http4sJwtAuth  = "dev.profunktor"     %% "http4s-jwt-auth" % Versions.http4sJwtAuth
    val catsRetry      = "com.github.cb372"   %% "cats-retry"      % Versions.catsRetry
    val cats           = "org.typelevel"      %% "cats-core"       % Versions.cats
    val catsEffect     = "org.typelevel"      %% "cats-effect"     % Versions.catsEffect
    val fs2            = "co.fs2"             %% "fs2-core"        % Versions.fs2
    val newtype        = "io.estatico"        %% "newtype"         % Versions.newtype
    val tsecPassHasher = "io.github.jmcardon" %% "tsec-password"   % Versions.tsec
    val log4cats       = "org.typelevel"      %% "log4cats-slf4j"  % Versions.log4cats
    val logback        = "ch.qos.logback"      % "logback-classic" % Versions.logback
    val monocleCore    = "dev.optics"         %% "monocle-core"    % Versions.monocle
    // Test
    val log4catsNoOp      = "org.typelevel"       %% "log4cats-noop"      % Versions.log4cats
    val refinedScalacheck = "eu.timepit"          %% "refined-scalacheck" % Versions.refined
    val weaverCats        = "com.disneystreaming" %% "weaver-cats"        % Versions.weaver
    val weaverDiscipline  = "com.disneystreaming" %% "weaver-discipline"  % Versions.weaver
    val weaverScalaCheck  = "com.disneystreaming" %% "weaver-scalacheck"  % Versions.weaver
    val testContainer     = "org.testcontainers"   % "postgresql"         % Versions.testContainer
    val postgresql        = "org.postgresql"       % "postgresql"         % Versions.postgresql
  }

  val circeLibs: Seq[ModuleID] = Seq(circeCore, circeGeneric, circeParser, circeRefined)

  val catsLibs: Seq[ModuleID] = Seq(cats, catsEffect, catsRetry)

  val http4sLibs: Seq[ModuleID] = Seq(http4sDsl, http4sServer, http4sClient, http4sCirce)

  val cirisLibs: Seq[ModuleID] = Seq(cirisRefined, cirisCore, cirisEnum)

  val logLibs: Seq[ModuleID] = Seq(log4cats, logback)

  val skunkLibs: Seq[ModuleID] = Seq(skunkCore, skunkCirce, skunkRefined)

  val derevoLibs: Seq[ModuleID] = Seq(derevoCore, derevoCats, derevoCirce)

  val s3Libraries: Seq[ModuleID] = Seq(awsCore, awsS3, awsSoftwareS3)

  val coreLibraries: Seq[ModuleID] =
    catsLibs ++ cirisLibs ++ circeLibs ++ skunkLibs ++ http4sLibs ++ logLibs ++ derevoLibs ++ s3Libraries ++
      Seq(
        fs2,
        refinedType,
        refinedCats,
        tsecPassHasher,
        redis4catsEffects,
        redis4catsLog4cats,
        guava,
        http4sJwtAuth,
        newtype,
        monocleCore,
        squants
      )

  val testLibraries: Seq[ModuleID] = s3Libraries ++ Seq(
    log4catsNoOp,
    refinedScalacheck,
    weaverCats,
    weaverDiscipline,
    weaverScalaCheck,
    testContainer,
    postgresql
  )
}
