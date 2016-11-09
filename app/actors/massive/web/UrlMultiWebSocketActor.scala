package actors.massive.web

import actors.massive.base.{ShutDownTime, Subscribe, BaseAutoShutdownActor, UnSubscribe}
import actors.massive.url.Url
import akka.actor.{Actor, Props, ActorRef}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 08/11/2016
 */

case class MultiInEvent(name : String, url : String)
case class MultiOutEvent(name : String, data : String)

object UrlMultiWebSocketActor {

  def props(out: ActorRef, lookupActor : ActorRef, subscribeOnActorName : String) = Props(new UrlMultiWebSocketActor(out, lookupActor, subscribeOnActorName))
}

class UrlMultiWebSocketActor(out: ActorRef, lookupActor : ActorRef, var subscribeOnActorName : String) extends Actor {

  val subscriptions = collection.mutable.Set[String]()

  override def aroundPreStart(): Unit = {
    context.system.scheduler.schedule(100 millisecond,10 second, self, "Hi there again!!!")
  }

  override def aroundPostStop(): Unit = {
    // UnSubscribe
    System.out.println(s"Multi UnSubscribe called!")

    def unsubscribeExtraSubscription(name: String): Unit = {
      val unSubscribe = UnSubscribe(name, BaseAutoShutdownActor.TOPIC_ALL_SUBSCRIPTION, context.self)
      lookupActor ! unSubscribe
    }
    subscriptions.map(unsubscribeExtraSubscription)
  }

  def receive = {
    case msg: String =>
      System.out.println(s"Multi Received msg String: $msg")
      out ! MultiOutEvent("None", "I received your message: " +  msg)
    case multiOut : MultiOutEvent =>
      System.out.println(s"Multi Received msg MultiOutEvent: $multiOut")
      out ! multiOut
    case in : MultiInEvent =>
      System.out.println(s"Multi Received msg InEvent: $in")
      out ! MultiOutEvent(in.name, "=== Subscribing to: "+ in.url)
      // Subscribe new subscription
      subscriptions.add(in.name)
      subscribeOnActorName = in.name
      val subscribe = Subscribe(subscribeOnActorName, BaseAutoShutdownActor.TOPIC_ALL_SUBSCRIPTION, context.self)
      lookupActor ! subscribe
      if(in.url!="") {
        val urlMess = Url(subscribeOnActorName, in.url, Some(60 seconds))
        lookupActor ! urlMess
        lookupActor ! ShutDownTime(subscribeOnActorName, 5 seconds)
      }

    case out : OutEvent =>
      System.out.println(s"Multi Received msg OutEvent: $out")
  }
}