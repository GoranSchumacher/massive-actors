package app

import actors.EchoActor
import actors.massive.base.ShutDownTime
import actors.massive.stringtest.StringTestPersistentLookupActor
import actors.massive.url.{URLPersistentLookupActor, Url}
import akka.actor.{DeadLetter, ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.duration.FiniteDuration

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 05/02/2016
 */
object URLPersistentActorApp extends App{

  import java.util.concurrent.TimeUnit
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  lazy val system = ActorSystem("example")
  //lazy val lookupActor = system.actorOf(Props[StringTestPersistentLookupActor], "StringTestPersistentLookupActor")
  lazy val lookupActor = system.actorOf(Props[URLPersistentLookupActor], "URLPersistentLookupActor")
  import scala.concurrent.duration._

  /////////// Start Deadletter Watcher
  val deadLettersSubscriber = system.actorOf(Props[EchoActor], name = "dead-letters-subscriber")
  val echoActor = system.actorOf(Props[EchoActor], name = "generic-echo-actor")

  system.eventStream.subscribe(deadLettersSubscriber, classOf[DeadLetter])
  /////////////////////////////////

  lookupActor ! ShutDownTime("Apple", 10 seconds)
  val url = Url("Apple", "http://apple.com", Some(1 minute))
  lookupActor ! url

}
