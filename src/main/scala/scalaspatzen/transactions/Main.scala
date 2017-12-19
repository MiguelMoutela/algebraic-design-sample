package scalaspatzen.transactions

import caseapp.{CaseApp, RemainingArgs}
import cats.implicits._

import scalaspatzen.transactions.interpreters._
import scalaspatzen.transactions.programs.AnalyzerServiceImpl

object Main extends CaseApp[CliOptions] {
  override def run(options: CliOptions, remainingArgs: RemainingArgs): Unit = {

    val programs = new AnalyzerServiceImpl(FileSystemInterpreter,
                                           BrowserInterpreter,
                                           AnalyzerInterpreter,
                                           ConfigInterpreter)

    import programs._

    generateReportAndOpenInBrowser(options.input, options.output).value
      .unsafeRunSync()
  }
}
