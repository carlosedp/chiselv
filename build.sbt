// See README.md for license details.

ThisBuild / organization := "com.carlosedp"
ThisBuild / description  := "ChiselV is a RISC-V core written in Chisel"
ThisBuild / homepage     := Some(url("https://carlosedp.com"))
ThisBuild / licenses     := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/carlosedp/chiselv"), "git@github.com:carlosedp/chiselv.git")
)
ThisBuild / developers := List(
  Developer("carlosedp", "Carlos Eduardo de Paula", "carlosedp@gmail.com", url("https://github.com/carlosedp"))
)
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"
Global / semanticdbEnabled                                 := true
Global / semanticdbVersion                                 := "4.4.28" //scalafixSemanticdb.revision // Force version due to compatibility issues
Global / onChangedBuildSource                              := ReloadOnSourceChanges

Compile / run / mainClass := Some("chiselv.Toplevel")
Test / logBuffered        := false

lazy val chiselv = (project in file("."))
  .settings(
    name         := "chiselv",
    version      := "1.0.0",
    scalaVersion := "2.13.6",
  )

// Default library versions
val defaultVersions = Map(
  "chisel3"          -> "3.5.0-RC1",
  "chiseltest"       -> "0.5-SNAPSHOT",
  "scalatest"        -> "3.2.10",
  "organize-imports" -> "0.5.0",
  "scalautils"       -> "0.7.0",
)

// Import libraries
libraryDependencies += "edu.berkeley.cs"  %% "chisel3"    % defaultVersions("chisel3")
libraryDependencies += ("edu.berkeley.cs" %% "chiseltest" % defaultVersions("chiseltest") % "test").changing()

libraryDependencies += "org.scalatest"                     %% "scalatest"        % defaultVersions("scalatest") % "test"
libraryDependencies += "com.carlosedp"                     %% "scalautils"       % defaultVersions("scalautils")
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % defaultVersions("organize-imports")
addCompilerPlugin(("edu.berkeley.cs"                        % "chisel3-plugin"   % defaultVersions("chisel3")).cross(CrossVersion.full))

// Aliases
addCommandAlias("com", "all compile test:compile")
addCommandAlias("rel", "reload")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll;all Compile / scalafix; Test / scalafix")
addCommandAlias("fix", "all Compile / scalafixAll; Test / scalafixAll")
addCommandAlias("lint", "fmt;fix")
addCommandAlias("deps", "dependencyUpdates")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-language:reflectiveCalls",
  "-feature",
  "-Xcheckinit",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-unused",
)
