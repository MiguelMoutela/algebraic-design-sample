package scalaspatzen.transactions.programs

import cats.Monoid
import cats.implicits._

import scalaspatzen.transactions.algebra._
import scalaspatzen.transactions.interpreters.ErrorOrIO
import scalaspatzen.transactions.model._

class AnalyzerServiceImpl(
    fs: FileSystem[ErrorOrIO],
    browser: Browser[ErrorOrIO],
    analyzer: Analyzer[Debitor,
                       Payment,
                       BigDecimal,
                       (BigDecimal, List[MonthlyFees]),
                       ComparisonResult],
    resources: Resources[ErrorOrIO, Environment],
    pdfConverter: PdfConverter[ErrorOrIO, String],
    formatter: Formatter[Map[Debitor, List[ComparisonResult]]])(
    implicit m: Monoid[BigDecimal])
    extends AnalyzerService[ErrorOrIO, Map[Debitor, List[ComparisonResult]]] {

  def generateHtmlReport(
      inputDir: String): ErrorOrIO[Map[Debitor, List[ComparisonResult]]] = {
    import analyzer._
    import resources._
    import fs._

    def analyze(debitors: List[Debitor],
                paymentsDueDayOfMonth: Int,
                payableAmounts: (BigDecimal, List[MonthlyFees]))
      : List[RawLine] => ComparisonResults =
      decodeLines andThen
        groupByDebitor(debitors) andThen
        groupByTimeInterval(paymentsDueDayOfMonth) andThen
        compare(debitors, payableAmounts)

    for {
      files    <- listFiles(inputDir)
      csvFiles = files.filter(path => path.endsWith(".csv"))
      rawLines <- csvFiles.traverse(readAllLines("Windows-1250"))
      c        <- getConfig
    } yield
      analyze(c.debitors,
              c.paymentsDueDayOfMonth,
              (c.yearlyFee, c.monthlyFees))(rawLines.flatten)
  }

  def saveHtmlReport(report: String, filename: String): ErrorOrIO[Unit] = {
    fs.writeAllText(report, filename)
  }

  def openHtmlReportInBrowser(filename: String): ErrorOrIO[Unit] = {
    browser.openFile(filename)
  }

  def generateReportAndOpenInBrowser(input: String,
                                     output: String): ErrorOrIO[Unit] = {
    import formatter._
    import resources._

    for {
      report <- generateHtmlReport(input)
      md     = toMarkdown(report)
      css    <- getCss
      html   = markdownToHtml(md, css)
      _      <- saveHtmlReport(html, s"$output.html")
      _      <- exportToPdf(html, s"$output.pdf")
      _      <- openHtmlReportInBrowser(s"$output.html")
    } yield ()
  }

  def exportToPdf(html: String, filename: String): ErrorOrIO[Unit] = {
    pdfConverter.exportToPdf(html, filename)
  }
}