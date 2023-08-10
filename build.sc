import mill._, mill.scalalib._, mill.scalalib.publish._
import scalafmt._
import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`com.carlosedp::mill-aliases::0.4.1`
import com.carlosedp.aliases._

object versions {
  val scala          = "2.13.10"
  val chisel         = "5.0.0"
  val chiseltest     = "5.0.0"
  val scalatest      = "3.2.16"
  val riscvassembler = "1.9.0"
  val mainargs       = "0.5.1"
  val oslib          = "0.9.1"
}

trait BaseProject extends ScalaModule with ScalafixModule with ScalafmtModule {
  def scalaVersion = versions.scala
  def ivyDeps = Agg(
    ivy"com.carlosedp::riscvassembler:${versions.riscvassembler}",
    ivy"com.lihaoyi::mainargs:${versions.mainargs}",
    ivy"org.chipsalliance::chisel:${versions.chisel}",
  )

  def scalacPluginIvyDeps = Agg(ivy"org.chipsalliance:::chisel-plugin:${versions.chisel}")

  object test extends ScalaTests with TestModule.ScalaTest {
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:${versions.scalatest}",
      ivy"edu.berkeley.cs::chiseltest:${versions.chiseltest}",
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
  def testall  = alias("__.test")
}
