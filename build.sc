import mill._, mill.scalalib._, mill.scalalib.publish._
import mill.scalalib.TestModule.ScalaTest
import scalafmt._
import coursier.maven.MavenRepository
import $ivy.`com.goyeau::mill-scalafix::0.2.10`
import com.goyeau.mill.scalafix.ScalafixModule

object versions {
  val scala           = "2.13.8"
  val chisel3         = "3.5.4"
  val chiseltest      = "0.5.4"
  val scalatest       = "3.2.13"
  val organizeimports = "0.6.0"
  val semanticdb      = "4.5.13"
  val riscvassembler  = "1.1.1"
  val scalautils      = "0.10.2"
  val oslib           = "0.8.1"
}

trait BaseProject extends ScalaModule with PublishModule {
  def scalaVersion   = versions.scala
  def publishVersion = "1.0.0"
  def projectName    = "chiselv"
  def pomSettings = PomSettings(
    description = "ChiselV is a RISC-V core written in Chisel",
    organization = "com.carlosedp",
    url = "https://github.com/carlosedp/chiselv",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("carlosedp", "chiselv"),
    developers = Seq(
      Developer("carlosedp", "Carlos Eduardo de Paula", "https://github.com/carlosedp"),
    ),
  )

  def repositoriesTask = T.task { // Add snapshot repositories in case needed
    super.repositoriesTask() ++ Seq("oss", "s01.oss")
      .map(r => s"https://$r.sonatype.org/content/repositories/snapshots")
      .map(MavenRepository(_))
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.carlosedp::riscvassembler:${versions.riscvassembler}",
    ivy"com.carlosedp::scalautils:${versions.scalautils}",
    ivy"com.lihaoyi::os-lib:${versions.oslib}",
  )
}

trait HasChisel3 extends ScalaModule {
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"edu.berkeley.cs::chisel3:${versions.chisel3}",
  )
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    ivy"edu.berkeley.cs:::chisel3-plugin:${versions.chisel3}",
  )
  // TODO: Run in parallel
  object test extends Tests with TestModule.ScalaTest {
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest:${versions.scalatest}",
      ivy"edu.berkeley.cs::chiseltest:${versions.chiseltest}",
    )

    def testOne(args: String*) = T.command {
      super.runMain("org.scalatest.run", args: _*)
    }
  }
}

trait CodeQuality extends ScalafixModule with ScalafmtModule {
  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:${versions.organizeimports}")
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    ivy"org.scalameta:::semanticdb-scalac:${versions.semanticdb}",
  )
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
      "-Ywarn-unused",
      "-P:chiselplugin:genBundleElements",
    )
  }
}

// Toplevel commands
def lint(ev: eval.Evaluator) = T.command {
  mill.main.MainModule.evaluateTasks(
    ev,
    Seq("__.fix", "+", "mill.scalalib.scalafmt.ScalafmtModule/reformatAll", "__.sources"),
    mill.define.SelectMode.Separated,
  )(identity)
}
def deps(ev: eval.Evaluator) = T.command {
  mill.scalalib.Dependency.showUpdates(ev)
}

object chiselv extends BaseProject with HasChisel3 with CodeQuality with ScalacOptions {
  def mainClass = Some("chiselv.Toplevel")
}
object chiselv_rvfi extends BaseProject with HasChisel3 with CodeQuality with ScalacOptions {
  def mainClass = Some("chiselv.RVFITop")
  def sources   = T.sources(millSourcePath / os.up / "chiselv" / "src")
}
