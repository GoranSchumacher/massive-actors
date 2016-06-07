package actors.massive

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import actors.AkkaTestkitSpecs2Support
import actors.massive.base.{IsActorInCache, ShutDownTime}
import actors.massive.stock.{StockLookupActor, SumStock, GetStock, AddStock}
import akka.actor.{Props, ActorSystem}
import akka.cluster.pubsub.DistributedPubSubMediator.SubscribeAck
import akka.testkit.TestProbe
import akka.util.Timeout
import org.specs2.mutable._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.concurrent.duration._

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 29/01/2016
 */

class StockLookupActorSpec extends Specification {

  "A stockActor" should {
    "reply to a get message with correct number" in new AkkaTestkitSpecs2Support {
      val stockLookupActor = system.actorOf(Props[StockLookupActor], "StockLookup")
      val testProbe = TestProbe()

      testProbe.send(stockLookupActor, AddStock("AAPL", 2))
      testProbe.send(stockLookupActor, AddStock("AAPL", 3))
      testProbe.send(stockLookupActor, GetStock("AAPL"))
      testProbe.expectMsg(5 : Int)
    }
  }


  "A Non existent actor" should {
    "not be in the cache" in new AkkaTestkitSpecs2Support {
      val stockLookupActor = system.actorOf(Props[StockLookupActor], "StockLookup")
      val testProbe = TestProbe()

      testProbe.send(stockLookupActor, IsActorInCache("XYZ"))
      testProbe.expectMsg(None)
    }
  }


  "An actor that has been put dormant" should {
    "not be in the cache" in new AkkaTestkitSpecs2Support {
      val stockLookupActor = system.actorOf(Props[StockLookupActor], "StockLookup")
      val testProbe = TestProbe()

      testProbe.send(stockLookupActor, AddStock("XYZ2", 2))
//      testProbe.send(stockLookupActor, IsActorInCache("XYZ2"))
//      testProbe.expectMsg(Some[akka.actor.ActorRef])
      Thread.sleep(1000)
      testProbe.send(stockLookupActor, IsActorInCache("XYZ2"))
      testProbe.expectMsg(None)
    }
  }
//
//  "Sending messages to 100.000 actors" should {
//    "reply to a get message with correct number" in new AkkaTestkitSpecs2Support {
//      val stockLookupActor = system.actorOf(Props[StockLookupActor], "StockLookup")
//      val testProbe = TestProbe()
//      val NUM_ACTORS = 100000
//      (1 to NUM_ACTORS).par.map {i =>
//        testProbe.send(stockLookupActor, AddStock(s"AAPL-$i", i))
//      }
//      testProbe.send(stockLookupActor, GetStock(s"AAPL-$NUM_ACTORS"))
////      val timeout = Timeout(5, TimeUnit.MINUTES)
////      testProbe.expectNoMsg(10 seconds)
//      testProbe.expectMsg(NUM_ACTORS : Int)
//    }
//  }

  "Summing upp all actors" should {
    "reply to a get message with correct number" in new AkkaTestkitSpecs2Support {
      val stockLookupActor = system.actorOf(Props[StockLookupActor], "StockLookup")
      val testProbe = TestProbe()

      val NUM_ACTORS = 100
      (1 to NUM_ACTORS).map {i =>
        testProbe.send(stockLookupActor, AddStock(s"AAPL-$i", i))
      }
      Thread.sleep(1000)

      testProbe.send(stockLookupActor, SumStock())
     testProbe.expectMsg(252 : Int)

      Thread.sleep(1000)
    }
  }
}