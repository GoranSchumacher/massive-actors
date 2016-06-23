package app

import actors.DeadLetterActor
import actors.massive.base.{GetActorRef, LookupActorName, ShutDownTime}
import actors.massive.factorial.{FactorialRequest, FactorialPersistentLookupActor}
import actors.massive.url.{Url, URLPersistentLookupActor}
import akka.actor.{ActorRef, DeadLetter, Props, ActorSystem}
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
  import scala.concurrent.duration._

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
  val ref : Future[ActorRef] = lookupActor.ask(GetActorRef("40")).mapTo[ActorRef]
  ref.map{ actor: ActorRef =>
    for(step <- 1 to 1000 ) {
      time("40! Direkt") {
        actor.ask(FactorialRequest("40")).map(x => println(x))
      }
      Thread.sleep(10000)
    }
  }

  time("40!") {
    lookupActor.ask(FactorialRequest("40"))
      //.map(res => println(s"Answer is: $res"))
  }
}