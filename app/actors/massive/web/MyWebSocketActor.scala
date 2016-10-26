package actors.massive.web

import actors.massive.base.{ShutDownTime, Subscribe, UnSubscribe}
import actors.massive.url.{URLPersistentLookupActor, Url}
import akka.actor._
import controllers.{InEvent, OutEvent}

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

  val subscriptions = collection.mutable.Set[String]()

  override def aroundPreStart(): Unit = {
    context.system.scheduler.schedule(100 millisecond,10 second, self, "Hi there again!!!")
  }

  override def aroundPostStop(): Unit = {
    // UnSubscribe
    System.out.println(s"UnSubscribe called!")

    def unsubscribeExtraSubscription(name: String): Unit = {
      val unSubscribe = UnSubscribe(name, URLPersistentLookupActor.TOPIC_ALL_SUBSCRIPTION, context.self)
      lookupActor ! unSubscribe
    }
    subscriptions.map(unsubscribeExtraSubscription)
  }

  def receive = {
    case msg: String =>
      System.out.println(s"Received msg String: $msg")
      out ! OutEvent("I received your message: " +  msg)
    case in : InEvent =>
      System.out.println(s"Received msg InEvent: $in")
      out ! OutEvent("=== Subscribing to: "+ in.url)
          // Subscribe new subscription
          subscriptions.add(in.name)
          subscribeOnActorName = in.name
          val subscribe = Subscribe(subscribeOnActorName, URLPersistentLookupActor.TOPIC_ALL_SUBSCRIPTION, context.self)
          lookupActor ! subscribe
      if(in.url!="") {
        val urlMess = Url(subscribeOnActorName, in.url, Some(60 seconds))
        lookupActor ! urlMess
        lookupActor ! ShutDownTime(subscribeOnActorName, 5 seconds)
      }

    case out : OutEvent =>
      System.out.println(s"Received msg OutEvent: $out")
  }
}
