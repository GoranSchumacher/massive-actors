package actors.massive.base

import java.util.concurrent.TimeUnit

import actors.massive.base._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import java.util.concurrent._

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 27/12/2015
 */

case class MessageAndSender(message: LookupActorName, sender: ActorRef)

case class ActorRefCache(actorRef: Option[akka.actor.ActorRef],
                         cachedMessages: List[MessageAndSender] = List[MessageAndSender](),
                         // Set of actors this actor subscribes to. Encoded as Actor.Path.
                         // these should be unregistered when shut down.
                         actorSubscriptions: scala.collection.mutable.Set[String] = scala.collection.mutable.LinkedHashSet[String]()
                          )

case class INTERNAL_RESULT_FROM_FINDER(actorRef: Try[akka.actor.ActorRef], stockRef: LookupActorName, originalSender: akka.actor.ActorRef)

case class IsActorInCache(override val name: String) extends LookupActorName

object BaseLookupActor {
  var actorRef: Option[ActorRef] = None
}

abstract class BaseLookupActor extends Actor with akka.actor.ActorLogging {
  val domain: String

  val actorProps: akka.actor.Props

  val delay_when_starting_actor: Option[akka.util.Timeout] = None

  implicit val timeout = Timeout(5, TimeUnit.MINUTES)

  override def aroundPreRestart(reason: scala.Throwable, message: scala.Option[scala.Any]): Unit = {
    // Read in state here!
  }

  def receive = {

    case isActorInCache: IsActorInCache => {
      sender ! context.child(isActorInCache.name)
    }

    case shutDown: ShutDown => {
      System.out.println(s"shutDown received for actor: $shutDown, Sender: $sender CLOSING!!!")
      sender ! shutDown
    }

    case actorRefReply: LookupActorNameWithReply => {
      //REMEMBER; It seams that if the child dies, messages are automatically sent to parent, as we get deadletter as sender here sometimes.
      if (sender.toString.contains("deadLetters")) {
        println(s"DEAD_LETTER FOUND1")
      }
      // Currently do handle reply the same way as non-reply
      findOrCreateChildActor(actorRefReply)
    }

    case actorRef: LookupActorName => {
      findOrCreateChildActor(actorRef)
    }

    case mess => System.out.println(s"(BaseLookupActor): MESSAGE NOT MATCHED: $mess Sender: $sender")
  }

  private def findOrCreateChildActor(lookupActorName: LookupActorName): Unit = {
    context.child(lookupActorName.name).getOrElse {
      val actor = context.actorOf(actorProps, name = lookupActorName.name)
      actor ! "dummyMessage"
      actor
    }.tell(lookupActorName, sender)
  }

  def active(another: ActorRef): Actor.Receive = {
    case Terminated(`another`) => context.stop(self)
  }
}