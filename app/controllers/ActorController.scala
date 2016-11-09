package controllers

import java.io.{ByteArrayOutputStream, ByteArrayInputStream, InputStream}
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import actors.massive.base.{LookupActorName, ShutDownTime}
import actors.massive.stock.{StockLookupActor, GetStock, AddStock}
import actors.massive.stringtest.{Cmd, StringTestPersistentLookupActor}
import actors.massive.url.{Url, Print, URLPersistentLookupActor}
import actors.massive.web._
import actors.stateless.{HTMLCleanerURL, PDFRenderActor, HTMLCleanerActor}
import actors.traits.RouteSlipMessage
import akka.util.Timeout
import com.sksamuel.elastic4s.{RichSearchResponse, RichGetResponse, ElasticClient}
import play.api.i18n.MessagesApi
import play.api.libs.iteratee.Enumerator
import play.api.mvc._
import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import play.mvc.Result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 31/01/2016
 */


class ActorController @Inject()(val messagesApi: MessagesApi, system: ActorSystem) extends Controller {

  lazy val esClient = ElasticClient.remote("127.0.0.1", 9300)

  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  lazy val lookupActor = system.actorOf(Props[StringTestPersistentLookupActor], "StringTestPersistentLookupActor")
//  lazy val lookupActor = system.actorOf(Props[StockLookupActor], "StockLookupActor")

  lazy val urlLookupActor = system.actorOf(Props[URLPersistentLookupActor], "URLPersistentLookupActor")
  lazy val HTMLCleanerActor = system.actorOf(Props[HTMLCleanerActor], "HTMLCleaner")
  lazy val PDFRenderActor = system.actorOf(Props[PDFRenderActor], "PDFRenderActor")


  def startActor = Action { implicit request =>

    //val stockLookupActor = system.actorOf(Props[StockLookupActor], "StockLookup")
    lookupActor ! AddStock("AAPL", 55)
    lookupActor ! GetStock("AAPL")
    Ok("OK")
  }

  def test = Action { implicit request =>
    //val system = ActorSystem("example")
    //val lookupActor = system.actorOf(Props[URLPersistentLookupActor], "URLPersistentLookupActor")

    //val lookupActor = system.actorOf(Props[StockLookupActor], "StockLookupActor")

      import scala.concurrent.duration._
//      lookupActor ! Cmd("ShutDownTest", "Jens")
//      lookupActor ! ShutDownTime("ShutDownTest", 1 second)

//      lookupActor ! Print("ShutDownTest")
////      Thread.sleep(3000)
//
//      lookupActor ! Cmd(s"Array-1122", s"MAMMA")
//
//      lookupActor ! Cmd("AAPL", "hej")
//      //Thread.sleep(1000)
//      lookupActor ! Cmd("AAPL", "på")
//      //Thread.sleep(1000)
//      lookupActor ! Cmd("AAPL", "dej")
//      //Thread.sleep(1000)
//    //  persistentLookupActor ! "snap"
//      lookupActor ! Cmd("AAPL", "PAPPA")
//      lookupActor ! Cmd("AAPL", "MAMMA")
//      //Thread.sleep(1000)
//      lookupActor ! Print("AAPL")
//    //  Thread.sleep(1000)
//    //  persistentLookupActor ! "setWakeupTimer"
//    //  Thread.sleep(1000)
//      lookupActor ! Cmd("AAPL", "Goran")
//      //Thread.sleep(1000)
//      lookupActor ! "print"


    //  lookupActor ! Cmd(s"Array-1000", s"Goran-1000")

    val start = new Date().getTime
    (1 to 100 ).map { i =>
      lookupActor ! Cmd(s"Array-$i", s"Command-$i")
      (1 to 100).map { j =>
        lookupActor ! Cmd(s"Array-$i", s"$j")
      }
      lookupActor ! Print(s"Array-$i")
    }

//    lookupActor ! AddStock(s"AddTest", 1)
//    lookupActor ! AddStock(s"AddTest", 2)
//    lookupActor ! AddStock(s"AddTest", 3)
//    lookupActor ! AddStock(s"AddTest", 4)
//
//    //
//    (lookupActor ? GetStock(s"AddTest")).mapTo[Int].map{i => {
//      System.out.println(s"AddTest1 getStock: $i")
//    }}
//
//    // Times out because of dead letter
//    val addTestActor = (lookupActor ? GetStock(s"AddTest")).mapTo[Int]
//    val res = for{
//      addTestActorRes <- addTestActor
//    } yield addTestActorRes
//    res.map{i =>
//      System.out.println(s"AddTest2 getStock: $i")
//    }
//    res.onComplete{
//      case Success(s) => System.out.println(s"Success: $s")
//      case Failure(f) => System.out.println(s"Success: $f")
//    }

    System.out.println(s"Duration: ${new Date().getTime()-start}")

    Thread.sleep(30000)

    Ok("OK")
  }

  def urlGet(url : String) = Action.async { implicit request =>
    import com.sksamuel.elastic4s.ElasticDsl._
    import com.sksamuel.elastic4s.jackson.ElasticJackson
    import ElasticJackson.Implicits._

    val future  =
      esClient.execute {
        search in "actor"->"URLPersistentActor" query {
          bool {
            must(
              termQuery("_id", url)
            )
          }
        }
      }
    future.map{t => Ok(t.as[actors.massive.url.MyState].apply(0).event.data).as("text/html")}
  }

  /////////////////////////////////////
  //////////////// URL ////////////////
  /////////////////////////////////////
  def url(url : String, min : Long) = Action { implicit request =>
    import scala.concurrent.duration._
    // Url("apple.com", "http://apple.com", Some(1 minutes))
    val urlMess = Url(url, s"http://$url", Some(min minutes))
    urlLookupActor ! urlMess
    Ok(views.html.url_actor(url))
  }

  def urlEmptyForm = Action { implicit request =>
    Ok(views.html.url_actor("empty"))
  }

  import play.api.libs.json._

  implicit val multiInEventFormat = Json.format[MultiInEvent]
  implicit val multiOutEventFormat = Json.format[MultiOutEvent]

  import play.api.mvc.WebSocket.FrameFormatter

  implicit val multiInEventFrameFormatter = FrameFormatter.jsonFrame[MultiInEvent]
  implicit val multiOutEventFrameFormatter = FrameFormatter.jsonFrame[MultiOutEvent]

  import play.api.mvc._
  import play.api.Play.current
  def urlActorSocket(urlName : String) = WebSocket.acceptWithActor[MultiInEvent, MultiOutEvent] { request => out =>
    System.out.println(s"ActorController.urlActorSocketMulti called urlName: $urlName")
    UrlWebSocketActor.props(out, urlLookupActor, urlName)
  }


  /////////////////////////////////////
  //////////////// URLPDF ///////////
  /////////////////////////////////////
  def urlGetToPDF(url : String) = Action.async { implicit request =>
    val aHTMLCleanerURL = HTMLCleanerURL(url)
    val routeSlipMessage = RouteSlipMessage(Seq(PDFRenderActor), aHTMLCleanerURL, true)
    import akka.pattern.ask
    ask(HTMLCleanerActor, routeSlipMessage).map{
      case a:HTMLCleanerURL =>
        Ok(a.byteArray.get).as("application/pdf")
    }
  }

  def urlPDF(url : String) = Action.async { implicit request =>
    import com.sksamuel.elastic4s.ElasticDsl._
    import com.sksamuel.elastic4s.jackson.ElasticJackson
    import ElasticJackson.Implicits._

    val future  =
      esClient.execute {
        search in "actor"->"URLPersistentActor" query {
          bool {
            must(
              termQuery("_id", url)
            )
          }
        }
      }

    val dbResponse: Future[Url] = future.map(_.as[actors.massive.url.MyState].apply(0).event)
    import akka.pattern.ask
    dbResponse.map{case a:Url=> ask(PDFRenderActor, HTMLCleanerURL(a.url, Some(a.data)))}.map{case a: HTMLCleanerURL =>
      Ok(a.byteArray.get).as("application/pdf")
    }
  }

}
