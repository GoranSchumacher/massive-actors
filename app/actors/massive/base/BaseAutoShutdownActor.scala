package actors.massive.base

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, DiagnosticActorLogging}
import akka.cluster.pubsub.DistributedPubSub
import akka.util.Timeout
import org.joda.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 29/12/2015
 */

abstract class LookupActorName{
  def name : String
}

abstract class LookupActorNameWithReply extends LookupActorName

case class GetActorRef(override val name : String) extends LookupActorNameWithReply
// Called to set the timeout time for auto shutdown.
case class ShutDownTime(override val name : String, shutdownTime : Duration) extends LookupActorName

case class ActorReference(actorRef : ActorRef)

// Called automatically
case class ShutDownRequest()
case class ShutDown(ref : ActorRef)

case class Subscribe(override val name : String = "EMPTY", topic : Int, subscriber : ActorRef) extends LookupActorName
case class UnSubscribe(override val name : String = "EMPTY", topic : Int, subscriber : ActorRef) extends LookupActorName

abstract class BaseAutoShutdownActor extends Actor with DiagnosticActorLogging {

  var domain : String

  var shutdownTime : Duration = Duration.Undefined
  var lastMessageTSMillis : Long = 0

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val topic = "chat"
  val mediator = DistributedPubSub(context.system).mediator

  val subscribers: scala.collection.mutable.Map[ActorRef, scala.collection.mutable.Set[Int]] = scala.collection.mutable.Map[ActorRef, scala.collection.mutable.Set[Int]]()

  def notifySubscribers(topic : Int, message : Any) = {
    System.out.println(s"In notifySubscribers $subscribers")
    subscribers.map{ s =>
      if(s._2.contains(topic) || s._2.contains(0)) {
        System.out.println(s"notifySubscribersInt:  subscriber: ${s}")
        s._1 ! message
      }
    }
    System.out.println(s"Out notifySubscribersInt")
  }

  def messageHandled(): Unit = {
    lastMessageTSMillis = DateTimeUtils.currentTimeMillis()
  }

  def hasSubscribers = {
    !subscribers.isEmpty
  }
  def receive : Receive = {

//    case ActorReference(actorRef: ActorRef) =>
//      context.watch(actorRef)
//      messageHandled()

    case shutDownRequest: ShutDownRequest => {
      System.out.println(s"Actor $self.path shutDownRequest, RECEIVED!!!")
      if (shutdownTime.isFinite() && lastMessageTSMillis != 0) {
        // This will always be true
        if (((lastMessageTSMillis + shutdownTime.toMillis) < DateTimeUtils.currentTimeMillis()) &&
          !hasSubscribers) {
          System.out.println(s"Actor $self.path shutDownRequest, shutting down.")
          context.parent ! ShutDown(self)
        } else {
          val dur: Duration = shutdownTime * 2
          context.system.scheduler.scheduleOnce(dur.asInstanceOf[FiniteDuration], self, ShutDownRequest())
        }
      }
      messageHandled()
    }
    case shutDownTime: ShutDownTime => {
      System.out.println(s"Actor $self.path ShutDownTime, RECEIVED!!!")
      shutdownTime = shutDownTime.shutdownTime
      val dur: Duration = shutdownTime * 2
      context.system.scheduler.scheduleOnce(dur.asInstanceOf[FiniteDuration], self, ShutDownRequest())
      messageHandled()
    }

    case shutDown: ShutDown => {
      System.out.println(s"Actor $self.path shutDownRequest, FINALLY shutting down!!!")
      context.stop(self)
    }

    case getActorRef: GetActorRef => {
      System.out.println(s"Actor $self.path  GetActorReference!")
      sender() ! ActorReference(context.self)
      messageHandled()
    }

    case subscribe : Subscribe => {
      System.out.println(s"Subscribe:  subscriber: ${subscribe.subscriber}")
      val x = subscribers.get(subscribe.subscriber)
      val x2 = x.getOrElse(scala.collection.mutable.Set[Int]())
      x2.add(subscribe.topic)
      subscribers.put(subscribe.subscriber, x2)
    }

    case unSubscribe : UnSubscribe => {
      System.out.println(s"UnSubscribe:  subscriber: ${unSubscribe.subscriber}")
      val x = subscribers.get(unSubscribe.subscriber)
      val x2 = x.getOrElse(scala.collection.mutable.Set[Int]())
      x2.remove(unSubscribe.topic)
      if(x2.size == 0) {
        subscribers.remove(unSubscribe.subscriber)
      } else {
        subscribers.put(unSubscribe.subscriber, x2)
      }
    }
  }
}
