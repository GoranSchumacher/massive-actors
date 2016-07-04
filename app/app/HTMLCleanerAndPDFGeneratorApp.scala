package app

import actors.DeadLetterActor
import actors.stateless.{PDFRenderActor, HTMLCleanerActor, HTMLCleanerURL}
import actors.traits.RouteSlipMessage
import akka.actor.{ActorSystem, DeadLetter, Props}
import akka.util.Timeout

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 26/06/2016
 */
object HTMLCleanerAndPDFGeneratorApp extends App {

  import java.util.concurrent.TimeUnit
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  lazy val system = ActorSystem("example")


  /////////// Start Deadletter Watcher
  val deadLettersSubscriber = system.actorOf(Props[DeadLetterActor], name = "dead-letters-subscriber")
  val echoActor = system.actorOf(Props[DeadLetterActor], name = "generic-echo-actor")

  system.eventStream.subscribe(deadLettersSubscriber, classOf[DeadLetter])
  /////////////////////////////////

  lazy val HTMLCleanerActor = system.actorOf(Props[HTMLCleanerActor], "HTMLCleaner")
  lazy val PDFRenderActor = system.actorOf(Props[PDFRenderActor], "PDFRenderActor")
  val aHTMLCleanerURL = HTMLCleanerURL("http://www.ikea.com/dk/da/catalog/categories/departments/dining/")
  import akka.pattern.ask

  import scala.concurrent.ExecutionContext.Implicits.global
  var out: String = _
  ask(HTMLCleanerActor, aHTMLCleanerURL).map { case x: HTMLCleanerURL =>
    out = x.result.get
    println(s"HTML: $x")

    ask(PDFRenderActor, HTMLCleanerURL(aHTMLCleanerURL.url, Some(out))).map{x => println(s"PDF: $x")}
  }

  // Stitch both actor calls together with a RouteSlip message
  val routeSlipMessage = RouteSlipMessage(Seq(PDFRenderActor), aHTMLCleanerURL, true)
  import akka.pattern.ask
  ask(HTMLCleanerActor, routeSlipMessage).map{
    case a:HTMLCleanerURL =>
      println(s"HTML: ${a.result.get}")
  }


}