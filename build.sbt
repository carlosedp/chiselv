// See README.md for license details.

ThisBuild / organization := "com.carlosedp"
ThisBuild / description  := "ChiselV is a RISC-V core written in Chisel"
ThisBuild / homepage     := Some(url("https://carlosedp.com"))
ThisBuild / licenses     := Seq("BSD 3-Clause" -> url("https://github.com/carlosedp/chiselv/blob/main/LICENSE"))
ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/carlosedp/chiselv"), "git@github.com:carlosedp/chiselv.git")
)
ThisBuild / developers := List(
  Developer("carlosedp", "Carlos Eduardo de Paula", "carlosedp@gmail.com", url("https://github.com/carlosedp"))
)

Global / semanticdbEnabled    := true
Global / semanticdbVersion    := scalafixSemanticdb.revision
Global / onChangedBuildSource := ReloadOnSourceChanges

Compile / run / mainClass := Some("chiselv.Toplevel")

lazy val chiselv = (project in file("."))
  .settings(
    name         := "chiselv",
    version      := "1.0.0",
    scalaVersion := "2.13.8"
  )

// Default library versions
lazy val versions = new {
  val chisel3         = "3.5.3"
  val chiseltest      = "0.5.3"
  val scalatest       = "3.2.12"
  val organizeimports = "0.6.0"
  val scalautils      = "0.10.2"
  val oslib           = "0.8.1"
}

// Import libraries
libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "chisel3"    % versions.chisel3,
  "edu.berkeley.cs" %% "chiseltest" % versions.chiseltest % "test",
  "org.scalatest"   %% "scalatest"  % versions.scalatest  % "test",
  "com.carlosedp"   %% "scalautils" % versions.scalautils,
  "com.lihaoyi"     %% "os-lib"     % versions.oslib
)
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % versions.organizeimports
addCompilerPlugin(("edu.berkeley.cs"                        % "chisel3-plugin"   % versions.chisel3).cross(CrossVersion.full))

// Aliases
addCommandAlias("com", "all compile test:compile")
addCommandAlias("rel", "reload")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll;all Compile / scalafix; Test / scalafix")
addCommandAlias("fix", "all Compile / scalafixAll; Test / scalafixAll")
addCommandAlias("lint", "fmt;fix")
addCommandAlias("deps", "dependencyUpdates")

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-language:reflectiveCalls",
  "-feature",
  "-Xcheckinit",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-unused",
  "-P:chiselplugin:genBundleElements"
)
