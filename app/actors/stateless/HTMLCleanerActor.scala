package actors.stateless

import java.net.URL

import actors.traits.{RouteSlipMessage, RouteSlip}
import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.event.LoggingReceive
import org.htmlcleaner._

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 24/06/2016
 */

case class HTMLCleanerURL(url: String, result: Option[String] = None)

class HTMLCleanerActor extends Actor with RouteSlip with ActorLogging {

  def receive = LoggingReceive {

    case clean: HTMLCleanerURL =>
      val result: HTMLCleanerURL = cleanHTML(clean)
      sender() ! result

    case routeSlip: RouteSlipMessage =>
      routeSlip.message match {
        case clean: HTMLCleanerURL => {
          val result: HTMLCleanerURL = cleanHTML(clean)
          sendMessageToNextTask(sender(), routeSlip, result)
        }
      }
  }

  def cleanHTML(clean: HTMLCleanerURL): HTMLCleanerURL = {
    val cleaner: HtmlCleaner = new HtmlCleaner()
    val url = new URL(clean.url)
    val urlHost = url.getHost
    val urlPath = url.getPath

    val node: TagNode = cleaner.clean(new URL(clean.url))
    node.traverse(new TagNodeVisitor() {
      override def visit(tagNode: TagNode, htmlNode: HtmlNode): Boolean = {
        if (htmlNode.isInstanceOf[TagNode]) {
          val tag: TagNode = htmlNode.asInstanceOf[TagNode]
          val tagName: String = tag.getName()
          if ("img" == tagName || "script" == tagName) {
            val src: String = tag.getAttributeByName("src")
            if (src != null) {
              val newRef = makeAbsoluteRef(src, urlHost, urlPath)
              import scala.collection.JavaConverters._
              tag.setAttributes(Map("src" -> newRef).asJava)
            }
          } else if ("link" == tagName || "a" == tagName) {
            val href: String = tag.getAttributeByName("href")
            if (href != null) {
              val newRef = makeAbsoluteRef(href, urlHost, urlPath)
              import scala.collection.JavaConverters._
              tag.setAttributes(Map("href" -> newRef).asJava)
            }
          }
        }
        true
      }
    })

    val serializer: SimpleHtmlSerializer = new SimpleHtmlSerializer(cleaner.getProperties())
    val result = clean.copy(result = Some(serializer.getAsString(node)))
    result
  }

  def makeAbsoluteRef(ref: String, host: String, path: String): String = {
    if(ref.startsWith("/")) {
      s"http://$host$ref"
    } else if(ref.startsWith("http://")) {
      ref
    } else { //
      s"http://$host$path/$ref"
    }
  }
}
