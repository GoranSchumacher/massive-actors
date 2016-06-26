package actors.stateless

import java.io._

import actors.traits.{RouteSlip, RouteSlipMessage}
import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import io.github.cloudify.scala.spdf._

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 24/06/2016
 */
class PDFRenderActor extends Actor with RouteSlip with ActorLogging {

  def receive = LoggingReceive {

    case clean: HTMLCleanerURL =>
      val result: HTMLCleanerURL = renderHTMLAsPDF(clean)
      sender() ! result

    case routeSlip: RouteSlipMessage =>
      routeSlip.message match {
        case clean: HTMLCleanerURL => {
          val result: HTMLCleanerURL = renderHTMLAsPDF(clean)
          sendMessageToNextTask(sender(), routeSlip, result)
        }
      }
  }

  def renderHTMLAsPDF(clean: HTMLCleanerURL): HTMLCleanerURL = {
    println("0")
    val pdf = Pdf(new PdfConfig {
      orientation := Landscape
      pageSize := "Letter"
      marginTop := "1in"
      marginBottom := "1in"
      marginLeft := "1in"
      marginRight := "1in"
    })
    println("1")
    val outputStream = new ByteArrayOutputStream
    println("2")
    if (clean.result.isDefined) {
      pdf.run(clean.result.get, outputStream)
    } else {
      pdf.run(clean.url, outputStream)
    }
    println("3")
    outputStream.flush()
    outputStream.close()
    println("4")
    val out = outputStream.toString("UTF-8")
    println(s"5. $out")
    val result = clean.copy(result = Some(out))
    println("6")
    result
  }
}
