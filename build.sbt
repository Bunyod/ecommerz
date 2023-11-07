import Dependencies.*
import sbt.ThisBuild

ThisBuild / scalaVersion      := "2.13.11"
ThisBuild / scalacOptions ++= CompilerOptions.cOptions
ThisBuild / coverageEnabled   := true
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name    := "ecommerz",
    version := "0.0.1",
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
  .aggregate(inventory, `auth-service`, commons, organization, `sales-api`, integration)

lazy val `auth-service` = (project in file("auth-service"))
  .settings(
    libraryDependencies ++= authDependencies,
    coverageExcludedPackages := "<empty>;.*\\.utils\\..*"
  )
  .dependsOn(commons, organization)

lazy val commons = (project in file("commons"))
  .settings(
    libraryDependencies ++= commonDependencies
  )

lazy val organization = (project in file("organization"))
  .settings(
    libraryDependencies ++= organizationDependencies,
    coverageExcludedPackages := "<empty>;.*\\.utils\\..*"
  )
  .dependsOn(commons)

lazy val `sales-api` = (project in file("sales-api"))
  .settings(
    libraryDependencies ++= inventoryDependencies,
    coverageExcludedPackages := "<empty>;.*\\.resources\\..*;.*\\.services\\..*",
    coverageExcludedFiles    := ".*Bootstrap;.*Config;.*HttpApi"
  )
  .dependsOn(
    organization,
    inventory % "compile->compile;test -> test"
  )

lazy val inventory = (project in file("inventory-api"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name                     := "inventory-api",
    Compile / mainClass      := Some("com.bbr.commerz.inventory.Bootstrap"),
    dockerBaseImage          := "hseeberger/scala-sbt:eclipse-temurin-11.0.13_1.6.0_2.13.7",
    dockerExposedPorts ++= Seq(8080),
    makeBatScripts           := Seq(),
    dockerUpdateLatest       := true,
    libraryDependencies ++= inventoryDependencies,
    coverageExcludedPackages := "<empty>;.*\\.image\\..*",
    coverageExcludedFiles    := ".*MkAwsServices"
  )
  .dependsOn(
    commons,
    organization   % "test->test",
    `auth-service` % "compile->compile;test->test;"
  )

lazy val integration = (project in file("integration"))
  .settings(
    publish / skip           := true,
    Test / parallelExecution := false,
    libraryDependencies ++= integrationDependencies
  )
  .dependsOn(
    inventory      % "test->test",
    `auth-service` % "test->test",
    `sales-api`    % "test->test",
    commons
  )
