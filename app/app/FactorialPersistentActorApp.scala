package app

import actors.DeadLetterActor
import actors.massive.base.ShutDownTime
import actors.massive.factorial.{FactorialRequest, FactorialPersistentLookupActor}
import actors.massive.url.{Url, URLPersistentLookupActor}
import akka.actor.{DeadLetter, Props, ActorSystem}
import akka.util.Timeout

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
  util.Timer.time("10!") {
    lookupActor.ask(FactorialRequest("10")).map(res => println(s"Answer is: $res"))
  }

  util.Timer.time("20!") {
    lookupActor.ask(FactorialRequest("20")).map(res => println(s"Answer is: $res"))
  }

  util.Timer.time("40!") {
    lookupActor.ask(FactorialRequest("40")).map(res => println(s"Answer is: $res"))
  }
}