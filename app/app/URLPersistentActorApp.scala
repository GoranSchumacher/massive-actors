package app

import actors.massive.stringtest.StringTestPersistentLookupActor
import actors.massive.url.{URLPersistentLookupActor, Url}
import akka.actor.{ActorSystem, Props}
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
  val url = Url("Apple", "http://apple.com", Some(10 seconds))
  lookupActor ! url

}
