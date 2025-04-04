import mill._, mill.scalalib._, mill.scalalib.publish._
import scalafmt._
import $ivy.`com.goyeau::mill-scalafix::0.4.1`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`com.carlosedp::mill-aliases::0.6.0`
import com.carlosedp.aliases._

object versions {
  val scala          = "2.13.16"
  val chisel         = "6.7.0"
  val chiseltest     = "6.0.0"
  val scalatest      = "3.2.18"
  val riscvassembler = "1.9.1"
  val mainargs       = "0.7.0"
}

trait BaseProject extends ScalaModule with ScalafixModule with ScalafmtModule {
  def scalaVersion = versions.scala
  def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:${versions.chisel}",
    ivy"com.lihaoyi::mainargs:${versions.mainargs}",
  )

  def scalacPluginIvyDeps = Agg(ivy"org.chipsalliance:::chisel-plugin:${versions.chisel}")

  object test extends ScalaTests with TestModule.ScalaTest {
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:${versions.scalatest}",
      ivy"edu.berkeley.cs::chiseltest:${versions.chiseltest}",
      ivy"com.carlosedp::riscvassembler:${versions.riscvassembler}",
    )
  }

  override def scalacOptions = T {
    super.scalacOptions() ++ Seq(
      "-unchecked",
      "-deprecation",
      "-language:reflectiveCalls",
      "-encoding",
      "UTF-8",
      "-feature",
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Ywarn-dead-code",
      "-Ywarn-unused",
      "-Ymacro-annotations",
    )
  }
}

object chiselv extends BaseProject {
  def mainClass = Some("chiselv.Toplevel")
}
object chiselv_rvfi extends BaseProject {
  def mainClass = Some("chiselv.RVFI")
  def sources   = T.sources(millSourcePath / os.up / "chiselv" / "src")
}

// -----------------------------------------------------------------------------
// Command Aliases
// -----------------------------------------------------------------------------
object MyAliases extends Aliases {
  def lint     = alias("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources", "__.fix")
  def fmt      = alias("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources")
  def checkfmt = alias("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources")
  def deps     = alias("mill.scalalib.Dependency/showUpdates")
  def test     = alias("__.test")
}
