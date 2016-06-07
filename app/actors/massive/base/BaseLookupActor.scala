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

case class MessageAndSender(message : LookupActorName, sender : ActorRef)
case class ActorRefCache(actorRef : Option[akka.actor.ActorRef] ,
                         cachedMessages : scala.collection.mutable.Set[MessageAndSender] = scala.collection.mutable.LinkedHashSet[MessageAndSender](),
                         // Set of actors this actor subscribes to. Encoded as Actor.Path.
                         // these should be unregistered when shut down.
                         actorSubscriptions : scala.collection.mutable.Set[String] = scala.collection.mutable.LinkedHashSet[String]()
                          )

case class INTERNAL_RESULT_FROM_FINDER(actorRef : Try[akka.actor.ActorRef], stockRef : LookupActorName, originalSender : akka.actor.ActorRef)

case class IsActorInCache(override val name : String) extends LookupActorName

object BaseLookupActor {
  var actorRef : Option[ActorRef] = None
}

abstract class BaseLookupActor extends Actor with akka.actor.ActorLogging  {

  val domain : String

  val actorProps : akka.actor.Props

  val delay_when_starting_actor : Option[akka.util.Timeout] = None

  implicit val timeout = Timeout(5, TimeUnit.MINUTES)

  val actorRefCache : scala.collection.mutable.Map[String, ActorRefCache] = scala.collection.mutable.Map[String, ActorRefCache]()

  override def aroundPreRestart(reason : scala.Throwable, message : scala.Option[scala.Any]): Unit = {
    // Read in state here!
    //StockLookupActor.actorRef = Some(context.self)
  }

  def receive = {

    case isActorInCache : IsActorInCache  => {
      val x = actorRefCache.get(isActorInCache.name)
      if(x.isDefined) {
        sender ! x.get.actorRef
      } else {
        sender ! None
      }
    }

    case shutDown : ShutDown  => {
      System.out.println(s"shutDown received for actor: $shutDown, Sender: $sender")
      actorRefCache.remove(shutDown.ref.path.name)
      System.out.println(s"shutDown received for actor: $shutDown, Sender: $sender CLOSING!!!")
      sender ! shutDown

    }
    case fromFinder : INTERNAL_RESULT_FROM_FINDER => {
      findOrCreateAndForward2Actor(fromFinder.actorRef, fromFinder.stockRef, fromFinder.originalSender)
    }

    case stockRefReply : LookupActorNameWithReply => {
      findOrCreateAndForwardActor(stockRefReply, sender)
    }
    case stockRef : LookupActorName => {
      findOrCreateAndForwardActor(stockRef, sender)
    }

    case ActorReference(actorRef : ActorRef) => {
      actorRef ! ActorReference(self)
    }

    case mess => System.out.println(s"(BaseLookupActor): MESSAGE NOT MATCHED: $mess Sender: $sender")
  }

  def active(another: ActorRef): Actor.Receive = {
    case Terminated(`another`) => context.stop(self)
  }

  def findOrCreateAndForwardActor(stockRef: LookupActorName, originalSender : ActorRef) = {

    val cacheEntry: Option[ActorRefCache] = actorRefCache.get(stockRef.name)

    // If No match in Cache => Look it up in Actor System
    if (cacheEntry.isEmpty) {

      // GS MOVED from inside future code block!!!!!
      //actorRefCache.put(stockRef.stock, ActorRefCache(sel)) // THis is not the one to add here!!!
      actorRefCache.put(stockRef.name, ActorRefCache(None))

      val selection: ActorSelection = context.actorSelection(stockRef.name)
      val sel: Future[akka.actor.ActorRef] = selection.resolveOne() // Returns a future!!
      //          sel.onFailure {
      //            case t => System.out.println(s"Actor $sel failed with message $t.message")
      //          }

      sel.onComplete{
        self ! INTERNAL_RESULT_FROM_FINDER(_, stockRef, originalSender)
      }

    } else {
      //System.out.println(s"findOrCreateAndForwardActor cacheEntry.isNOTEmpty to $stockRef, Sender: $sender")
      // It is in the cache
      val cache: ActorRefCache = cacheEntry.get

      //System.out.println(s"findOrCreateAndForwardActor cache $cache Time: ${new Date().getTime}")
      val actorRef = cache.actorRef

      if (actorRef.isDefined) {
        //System.out.println(s"isCompleted.Success executed for actor: $actorRef Time: ${new Date().getTime}")

        actorRef.map { actor =>
          actor.tell(stockRef, originalSender)
          System.out.println(s"Tell4: $stockRef")
        }
      } else {
        //System.out.println(s"isNOTCompleted.Success executed for actor: $actorRef")
        actorRefCache.put(stockRef.name, cache.copy(cachedMessages = cache.cachedMessages += MessageAndSender(stockRef, originalSender))) // ADDED MESSAGE
      }

    }
  }

  def findOrCreateAndForward2Actor(possible: Try[ActorRef], message : LookupActorName, sender : ActorRef)  = {
    //System.out.println(s"findOrCreateAndForward2Actor, Sender: $sender ")
    val definite = possible match {
      case Success(actor) =>
        //System.out.println("Found an actor " + isin)
        actorRefCache.get(message.name).map{cache =>
          actorRefCache.put(message.name, cache.copy(actorRef = Some(actor)))
        }
        actor.tell(message, sender)
        System.out.println(s"Tell3: $message")
      case Failure(errorMessage) =>
        //System.out.println(s"Creating a new actor: $isin, Error: $errorMessage ")
        //TODO StockActor SHOULD BE A PARAMETER!!!!!
        val actor = context.actorOf(actorProps, name = message.name)
        actorRefCache.get(message.name).map{cache =>
          //System.out.println(s"Updating actor: $actor ")
          if(delay_when_starting_actor.isDefined) {
            actor ! "dummyMessage"
            Thread.sleep(delay_when_starting_actor.get.duration.toMillis)
          }
          actor.tell(message, sender)
          System.out.println(s"Tell1: $message")
          cache.cachedMessages.foreach { mess =>
            //System.out.println(s"Sending cached messages2  Message: $mess")
            actor.tell(mess.message, mess.sender)
            System.out.println(s"  Tell2: $mess")
          }
          cache.cachedMessages.clear()
          actorRefCache.replace(message.name, cache.copy(actorRef = Some(actor)))
        }
    }
  }
}