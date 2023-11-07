import sbt.*

object Dependencies {

  object Versions {
    val cats             = "2.9.0"
    val catsEffect       = "3.4.8"
    val s3               = "1.12.470"
    val logback          = "1.4.7"
    val refined          = "0.10.3"
    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.13.2"
    val http4s           = "0.23.22"
    val circe            = "0.14.5"
    val circeExtras      = "0.14.3"
    val http4sJwtAuth    = "1.2.0"
    val log4cats         = "1.1.1"
    val log4catsNoOp     = "2.5.0"
    val catsRetry        = "3.1.0"
    val ciris            = "3.2.0"
    val pureConfig       = "0.17.4"
    val redis4cats       = "1.4.1"

    val scalaCheck = "1.17.0"

    val weaver  = "0.8.3"
    val doobie  = "1.0.0-RC4"
    val monocle = "3.2.0"

    val enumeratum       = "1.7.2"
    val enumeratumDoobie = "1.7.3"

    val squants = "1.8.3"
  }

  object Libraries {
    val cats       = "org.typelevel"    %% "cats-core"   % Versions.cats
    val catsEffect = "org.typelevel"    %% "cats-effect" % Versions.catsEffect
    val catsRetry  = "com.github.cb372" %% "cats-retry"  % Versions.catsRetry

    def http4s(artifact: String): ModuleID = "org.http4s"   %% s"http4s-$artifact" % Versions.http4s
    def circe(artifact: String): ModuleID  = "io.circe"     %% artifact            % Versions.circe
    def ciris(artifact: String): ModuleID  = "is.cir"       %% artifact            % Versions.ciris
    def doobie(artifact: String): ModuleID = "org.tpolecat" %% s"doobie-$artifact" % Versions.doobie

    val doobieCore          = doobie("core")
    val doobieH2            = doobie("h2") // H2 driver 1.4.200 + type mappings.
    val doobieHikari        = doobie("hikari") // HikariCP transactor.
    val doobiePostgres      = doobie("postgres") // Postgres driver 42.3.1 + type mappings.
    val doobiePostgresCirce = doobie("postgres-circe")
    val doobieRefined       = doobie("refined") // Support Refined Types
    val doobieTest          = doobie("scalatest") % "test" // ScalaTest support for typechecking statements.

    val redis4catsEffects  = "dev.profunktor" %% "redis4cats-effects"  % Versions.redis4cats
    val redis4catsLog4cats = "dev.profunktor" %% "redis4cats-log4cats" % Versions.redis4cats

    val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % Versions.s3

    val pureConfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig

    val circeCore    = circe("circe-core")
    val circeGeneric = circe("circe-generic")
    val circeExtras  = "io.circe"      %% "circe-generic-extras" % Versions.circeExtras
    val circeParser  = circe("circe-parser")
    val circeRefined = circe("circe-refined")
    val circeEnum    = "com.beachape"  %% "enumeratum-circe"     % Versions.enumeratum
    val doobieEnum   = ("com.beachape" %% "enumeratum-doobie"    % Versions.enumeratumDoobie)
      .excludeAll(ExclusionRule("org.tpolecat"))

    val weaverCats       = "com.disneystreaming" %% "weaver-cats"       % Versions.weaver % Test
    val weaverDiscipline = "com.disneystreaming" %% "weaver-discipline" % Versions.weaver % Test
    val weaverScalaCheck = "com.disneystreaming" %% "weaver-scalacheck" % Versions.weaver % Test

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val http4sDsl    = http4s("dsl")
    val http4sServer = http4s("ember-server")
    val http4sClient = http4s("ember-client")
    val http4sCirce  = http4s("circe")

    val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % Versions.http4sJwtAuth
    val monocleCore   = "dev.optics"     %% "monocle-core"    % Versions.monocle

    val refinedCore       = "eu.timepit" %% "refined"            % Versions.refined
    val refinedCats       = "eu.timepit" %% "refined-cats"       % Versions.refined
    val refinedPureconfig = "eu.timepit" %% "refined-pureconfig" % Versions.refined

    val log4cats     = ("io.chrisdavenport" %% "log4cats-slf4j" % Versions.log4cats)
      .excludeAll(ExclusionRule("org.typelevel"))
    val log4catsNoOp = "org.typelevel"      %% "log4cats-noop"  % Versions.log4catsNoOp

    // Runtime
    val logback    = "ch.qos.logback"  % "logback-classic" % Versions.logback    % Runtime
    val scalaCheck = "org.scalacheck" %% "scalacheck"      % Versions.scalaCheck % Test

    val squants = "org.typelevel" %% "squants" % Versions.squants

  }

  object CompilerPlugins {
    val betterMonadicFor = compilerPlugin("com.olegpy" %% "better-monadic-for" % Versions.betterMonadicFor)
    val kindProjector    = compilerPlugin(
      ("org.typelevel" %% "kind-projector" % Versions.kindProjector).cross(CrossVersion.full)
    )
  }

  val common: Seq[ModuleID] = Seq(
    compilerPlugin(CompilerPlugins.kindProjector.cross(CrossVersion.full)),
    compilerPlugin(CompilerPlugins.betterMonadicFor),
    CompilerPlugins.kindProjector,
    Libraries.squants
  )

  val cats: Seq[ModuleID] = Seq(
    Libraries.cats,
    Libraries.catsEffect,
    Libraries.catsRetry
  )

  val circe: Seq[ModuleID] = Seq(
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeExtras,
    Libraries.circeParser,
    Libraries.circeRefined,
    Libraries.circeEnum
  )

  val ciris: Seq[ModuleID] = Seq(
    Libraries.cirisCore,
    Libraries.cirisEnum,
    Libraries.cirisRefined
  )

  val doobie: Seq[ModuleID] = Seq(
    Libraries.doobieCore,
    Libraries.doobieH2,
    Libraries.doobieHikari,
    Libraries.doobiePostgres,
    Libraries.doobiePostgresCirce,
    Libraries.doobieRefined,
    Libraries.doobieEnum,
    Libraries.doobieTest
  )

  val http4s: Seq[ModuleID] = Seq(
    Libraries.http4sDsl,
    Libraries.http4sClient,
    Libraries.http4sServer,
    Libraries.http4sCirce,
    Libraries.http4sJwtAuth
  )

  val refined: Seq[ModuleID] = Seq(
    Libraries.refinedPureconfig,
    Libraries.refinedCore,
    Libraries.refinedCats
  )

  val redis: Seq[ModuleID] = Seq(
    Libraries.redis4catsEffects,
    Libraries.redis4catsLog4cats
  )

  val weaver: Seq[ModuleID] = Seq(
    Libraries.weaverCats,
    Libraries.weaverDiscipline,
    Libraries.weaverScalaCheck
  )

  val organizationDependencies: Seq[ModuleID] =
    common ++ cats ++ circe ++ ciris ++ doobie ++ http4s ++ refined ++ weaver

  val authDependencies: Seq[ModuleID] = organizationDependencies ++ redis :+ Libraries.scalaCheck

  val commonDependencies: Seq[ModuleID] = Seq(
    Libraries.monocleCore,
    Libraries.awsS3
  ) ++ organizationDependencies

  val integrationDependencies: Seq[ModuleID] = Seq(
    Libraries.pureConfig
  ) ++ common ++ cats ++ redis ++ weaver

  val inventoryDependencies: Seq[ModuleID] = Seq(
    Libraries.log4cats,
    Libraries.logback,
    Libraries.pureConfig,
    Libraries.awsS3,
    Libraries.log4catsNoOp
  ) ++ authDependencies
}
