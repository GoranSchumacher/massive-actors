package actors.stateless

import actors.traits.{RouteSlip, RouteSlipMessage}
import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import net.kaliber.pdf.PdfRenderer

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
    val renderer = new PdfRenderer(ClassLoader.getSystemClassLoader)
    val bytes = if (clean.result.isDefined) {
      renderer.toBytes(clean.result.get)
    } else {
      renderer.toBytes(clean.url)
    }
    clean.copy(result = None, byteArray=Some(bytes))
  }
}
