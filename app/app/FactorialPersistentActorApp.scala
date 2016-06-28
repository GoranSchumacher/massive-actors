package app

import actors.DeadLetterActor
import actors.massive.base.{ActorReference, GetActorRef}
import actors.massive.factorial.{FactorialPersistentLookupActor, FactorialRequest}
import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import akka.util.Timeout
import util.Timer.time

import scala.concurrent.Future

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 22/06/2016
 */
object FactorialPersistentActorApp extends App{

  import java.util.concurrent.TimeUnit
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  lazy val system = ActorSystem("example")
  //lazy val lookupActor = system.actorOf(Props[StringTestPersistentLookupActor], "StringTestPersistentLookupActor")
  lazy val lookupActor = system.actorOf(Props[FactorialPersistentLookupActor], "FactorialPersistentLookupActor")

  /////////// Start Deadletter Watcher
  val deadLettersSubscriber = system.actorOf(Props[DeadLetterActor], name = "dead-letters-subscriber")
  val echoActor = system.actorOf(Props[DeadLetterActor], name = "generic-echo-actor")

  system.eventStream.subscribe(deadLettersSubscriber, classOf[DeadLetter])
  /////////////////////////////////

  import akka.pattern.ask

  import scala.concurrent.ExecutionContext.Implicits.global
  time("10!") {
    lookupActor.ask(FactorialRequest("10")).map(res => println(s"Answer is: $res"))
  }

  time("20!") {
    lookupActor.ask(FactorialRequest("20")).map(res => println(s"Answer is: $res"))
  }


  // Fetch an actor ref and ask directly
  val ref = ask(lookupActor, GetActorRef("40"))
  ref.map{ case actorRef: ActorReference =>
    for(step <- 1 to 1000 ) {
      time("40! Direkt") {
        actorRef.actorRef.ask(FactorialRequest("40")).map(x => println(x))
      }
      Thread.sleep(10000)
    }
  }
//
//
//  // Fetch an actor ref and ask directly
//  ref.map{ case actorRef: ActorReference =>
//    time("40! Direkt 1.000 times") {
//      for(step <- 1 to 1000 ) {
//        actorRef.actorRef.ask(FactorialRequest("40")).map(x => println(x))
//      }
//    }
//  }
//
//  // Fetch an actor ref and ask directly
//  ref.map{ case actorRef: ActorReference =>
//      time("40! Direkt 1.000.000 times without waiting for reply") {
//        for(step <- 1 to 1000000 ) {
//          actorRef.actorRef.ask(FactorialRequest("40"))
//            //.map(x => println(x))
//        }
//      }
//  }
//
//  time("40!") {
//    lookupActor.ask(FactorialRequest("40")).map(x => println(x))
//  }

  Thread.sleep(10000)
}