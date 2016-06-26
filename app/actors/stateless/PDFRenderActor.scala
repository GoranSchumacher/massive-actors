package actors.stateless

import java.net.URL

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import org.htmlcleaner._
import io.github.cloudify.scala.spdf._
import java.io._
import java.net._

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 24/06/2016
 */
class PDFRenderActor extends Actor with ActorLogging {

  def receive = LoggingReceive {

    case clean: HTMLCleanerURL =>
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
      pdf.run(clean.result.get, outputStream)
      println("3")
      outputStream.close()
      println("4")
      val out = outputStream.toString("UTF-8")
      println("5")
      val result = clean.copy(result = Some(out))
      println("6")
      sender() ! result
  }
}
