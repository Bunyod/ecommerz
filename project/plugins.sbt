addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.2")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")

// check for unused code 1. sbt 2. unusedCode 3. scalafix WarnUnusedCode 
addSbtPlugin("com.github.xuwei-k" % "unused-code-plugin" % "0.3.0")