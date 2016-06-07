package actors.massive.web

import actors.massive.base.{ShutDownTime, UnSubscribe, Subscribe}
import actors.massive.url.{Url, URLPersistentLookupActor}
import akka.actor._
import controllers.{OutEvent, InEvent}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 09/02/2016
 */
object MyWebSocketActor {

  def props(out: ActorRef, lookupActor : ActorRef, subscribeOnActorName : String) = Props(new MyWebSocketActor(out, lookupActor, subscribeOnActorName))
}

class MyWebSocketActor(out: ActorRef, lookupActor : ActorRef, var subscribeOnActorName : String) extends Actor {

  override def aroundPreStart(): Unit = {
    context.system.scheduler.schedule(100 millisecond,10 second, self, "Hi there again!!!")
    // Subscribe
    val subscribe = Subscribe(subscribeOnActorName, URLPersistentLookupActor.TOPIC_SUBSCRIPTION_LENGTH, context.self)
    lookupActor ! subscribe
    lookupActor ! ShutDownTime(subscribeOnActorName, 5 seconds)
  }

  override def aroundPostStop(): Unit = {
    // UnSubscribe
    System.out.println(s"UnSubscribe called!")
    val unSubscribe = UnSubscribe(subscribeOnActorName, URLPersistentLookupActor.TOPIC_SUBSCRIPTION_LENGTH, context.self)
    lookupActor ! unSubscribe
  }

  def receive = {
    case msg: String =>
      System.out.println(s"Received msg String: $msg")
      out ! OutEvent("I received your message: " +  msg)
    case in : InEvent =>
      System.out.println(s"Received msg InEvent: $in")
      out ! OutEvent(in.url)
          // unsubscribe
          val unSubscribe = UnSubscribe(subscribeOnActorName, URLPersistentLookupActor.TOPIC_SUBSCRIPTION_LENGTH, context.self)
          lookupActor ! unSubscribe
          //
          // Subscribe
          subscribeOnActorName = in.name
          val subscribe = Subscribe(subscribeOnActorName, URLPersistentLookupActor.TOPIC_SUBSCRIPTION_LENGTH, context.self)
          lookupActor ! subscribe
          val urlMess = Url(subscribeOnActorName, in.url, Some(60 seconds))
          lookupActor ! urlMess
          lookupActor ! ShutDownTime(subscribeOnActorName, 5 seconds)
    case out : OutEvent =>
      System.out.println(s"Received msg OutEvent: $out")
  }
}
