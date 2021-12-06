import mill._
import mill.scalalib._
import scalafmt._
import coursier.maven.MavenRepository
import $ivy.`com.goyeau::mill-scalafix:0.2.4`
import com.goyeau.mill.scalafix.ScalafixModule

val defaultVersions = Map(
  "scala"            -> "2.13.6",
  "chisel3"          -> "3.5.0-RC1",
  "chiseltest"       -> "0.5-SNAPSHOT",
  "scalatest"        -> "3.2.10",
  "scalautils"       -> "0.7.2",
  "semanticdb"       -> "4.4.30",
  "organize-imports" -> "0.6.0",
  "os-lib"           -> "0.7.8"
)

trait BaseProject extends ScalaModule {
  def crossScalaVersion = defaultVersions("scala")
  def mainClass         = Some("chiselv.Toplevel")
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.carlosedp::scalautils:${defaultVersions("scalautils")}",
    ivy"com.lihaoyi::os-lib:${defaultVersions("os-lib")}"
  )
  def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(
      MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
      MavenRepository("https://s01.oss.sonatype.org/content/repositories/snapshots")
    )
  }
}

trait HasChisel3 extends ScalaModule {
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"edu.berkeley.cs::chisel3:${defaultVersions("chisel3")}",
    ivy"edu.berkeley.cs::firrtl:1.5-SNAPSHOT"
  )
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    ivy"edu.berkeley.cs:::chisel3-plugin:${defaultVersions("chisel3")}"
  )
}

trait HasChiselTests extends CrossSbtModule {
  object test extends Tests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest:${defaultVersions("scalatest")}",
      ivy"edu.berkeley.cs::chiseltest:${defaultVersions("chiseltest")}"
    )

    def testFramework = "org.scalatest.tools.Framework"

    def testOne(args: String*) = T.command {
      super.runMain("org.scalatest.run", args: _*)
    }
  }
}

trait CodeQuality extends ScalafixModule with ScalafmtModule {
  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:${defaultVersions("organize-imports")}")
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    ivy"org.scalameta:::semanticdb-scalac:${defaultVersions("semanticdb")}"
  )
}

trait Aliases extends Module {
  def fmt = T {
    toplevel.reformat()
  }
  def lint = T {
    toplevel.reformat()
    toplevel.fix()
  }
}

trait ScalacOptions extends ScalaModule {
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq(
      "-unchecked",
      "-deprecation",
      "-language:reflectiveCalls",
      "-feature",
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Ywarn-dead-code",
      "-Ywarn-unused"
    )
  }
}

object toplevel
  extends CrossSbtModule
  with BaseProject
  with HasChisel3
  with HasChiselTests
  with CodeQuality
  with Aliases
  with ScalacOptions {
  override def millSourcePath = super.millSourcePath
}
