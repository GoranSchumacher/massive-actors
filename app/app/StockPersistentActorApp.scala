package app

import java.util.Date
import java.util.concurrent.TimeUnit

import actors.EchoActor
import actors.massive.base.ShutDownTime
import actors.massive.stock.{AddStock, GetStock, StockLookupActor}
import actors.massive.stringtest.StringTestPersistentLookupActor
import actors.massive.url.{URLPersistentLookupActor, Print}
import akka.actor.{DeadLetter, Props, ActorSystem}
import akka.util.Timeout

import scala.util.{Failure, Success}
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 01/02/2016
 */
object StockPersistentActorApp extends App {

  lazy val system = ActorSystem("example")
  lazy val lookupActor = system.actorOf(Props[StockLookupActor], "StockLookupActor")

  val deadLettersSubscriber = system.actorOf(Props[EchoActor], name = "dead-letters-subscriber")
  val echoActor = system.actorOf(Props[EchoActor], name = "generic-echo-actor")

  system.eventStream.subscribe(deadLettersSubscriber, classOf[DeadLetter])

  //val lookupActor = system.actorOf(Props[StockLookupActor], "StockLookupActor")

  import scala.concurrent.duration._
//  lookupActor ! Cmd("ShutDownTest", "Jens")
  lookupActor ! ShutDownTime("ShutDownTest", 1 second)
//  lookupActor ! Print("ShutDownTest")
//  Thread.sleep(3000)
//
//  lookupActor ! Cmd("AAPL", "hej")
//  Thread.sleep(1000)
//  lookupActor ! Cmd("AAPL", "på")
//  Thread.sleep(1000)
//  lookupActor ! Cmd("AAPL", "dej")
//  Thread.sleep(1000)
////  persistentLookupActor ! "snap"
//  lookupActor ! Cmd("AAPL", "PAPPA")
//  lookupActor ! Cmd("AAPL", "MAMMA")
//  Thread.sleep(1000)
//  lookupActor ! Print("AAPL")
////  Thread.sleep(1000)
////  persistentLookupActor ! "setWakeupTimer"
////  Thread.sleep(1000)
//  lookupActor ! Cmd("AAPL", "Goran")
//  Thread.sleep(1000)
//  lookupActor ! "print"


//  lookupActor ! Cmd(s"Array-1000", s"Goran-1000")

  val start = new Date().getTime
  (1 to 100 ).map{ i =>

    lookupActor ! AddStock(s"Array-$i", i)
    //lookupActor ! AddStock(s"Array-$i", s"Goran-${i+1}")
    lookupActor ! GetStock(s"Array-$i")
  }

//  lookupActor ! AddStock(s"AddTest", 1)
//  lookupActor ! AddStock(s"AddTest", 2)
//  lookupActor ! AddStock(s"AddTest", 3)
//  lookupActor ! AddStock(s"AddTest", 4)
//  val addTestActor = (lookupActor ? GetStock(s"AddTest")).mapTo[Int]
//  val res = for{
//    addTestActorRes <- addTestActor
//  } yield addTestActorRes
//  res.map{i =>
//    System.out.println(s"AddTest getStock: $i")
//  }
//  res.onComplete{
//    case Success(s) => System.out.println(s"Success: $s")
//    case Failure(f) => System.out.println(s"Success: $f")
//  }

  System.out.println(s"Duration: ${new Date().getTime()-start}")

  Thread.sleep(30000)
  //system.shutdown()
  system.terminate()

}
