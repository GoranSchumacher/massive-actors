package controllers

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import actors.massive.base.{LookupActorName, ShutDownTime}
import actors.massive.stock.{StockLookupActor, GetStock, AddStock}
import actors.massive.stringtest.{Cmd, StringTestPersistentLookupActor}
import actors.massive.url.{Url, Print, URLPersistentLookupActor}
import actors.massive.web.MyWebSocketActor
import akka.util.Timeout
import com.sksamuel.elastic4s.{RichSearchResponse, RichGetResponse, ElasticClient}
import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import play.api.mvc.{Action, Controller}
import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 31/01/2016
 */

case class InEvent(name : String, url : String)
case class OutEvent(data : String)

class ActorController @Inject()(val messagesApi: MessagesApi, system: ActorSystem) extends Controller {

  lazy val esClient = ElasticClient.remote("127.0.0.1", 9300)

  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  //lazy val system2 = ActorSystem("example")
  lazy val lookupActor = system.actorOf(Props[StringTestPersistentLookupActor], "StringTestPersistentLookupActor")
//  lazy val lookupActor = system.actorOf(Props[StockLookupActor], "StockLookupActor")

  lazy val urlLookupActor = system.actorOf(Props[URLPersistentLookupActor], "URLPersistentLookupActor")

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

  def url(url : String, min : Long) = Action { implicit request =>
    import scala.concurrent.duration._
    // Url("apple.com", "http://apple.com", Some(1 minutes))
    val urlMess = Url(s"$url", s"http://$url", Some(min minutes))
    urlLookupActor ! urlMess
    Ok(views.html.url_actor(url))
  }

  def urlGet(url : String) = Action.async { implicit request =>
    import com.sksamuel.elastic4s.ElasticDsl._
    import com.sksamuel.elastic4s.jackson.ElasticJackson
    import ElasticJackson.Implicits._

    //val future : Future[RichSearchResponse] =
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
    //future.map{t => Ok(views.html.url_actor())}
  }

  import play.api.libs.json._

  implicit val inEventFormat = Json.format[InEvent]
  implicit val outEventFormat = Json.format[OutEvent]

  import play.api.mvc.WebSocket.FrameFormatter

  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[InEvent]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]

  import play.api.mvc._
  import play.api.Play.current
  def urlActorSocket(urlName : String) = WebSocket.acceptWithActor[InEvent, OutEvent] { request => out =>
    System.out.println(s"ActorController.urlActorSocket called urlName: $urlName")

    MyWebSocketActor.props(out, urlLookupActor, urlName)
  }

}
