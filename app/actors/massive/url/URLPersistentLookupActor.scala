package actors.massive.url

import java.util.concurrent.TimeUnit

import actors.massive.base.{BasePersistentAutoShutdownActor, BaseAutoShutdownActor, BaseLookupActor}
import actors.massive.stock.{StockLookupActor, StockActor, GetStock, SumStock}
import akka.actor.{ActorSystem, ActorRef, Actor, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 01/02/2016
 */
object URLPersistentLookupActor {

  //lazy val system = ActorSystem("example") // TODO This is not the same actorsystem as the Play actor system
  //lazy val lookupActor = system.actorOf(Props[URLPersistentLookupActor], "URLPersistentLookupActor")

   val TOPIC_ALL_SUBSCRIPTION = 0 // Length changed on url content
   val TOPIC_SUBSCRIPTION_LENGTH = 1 // Length changed on url content

   val domain = "URLPersistent"

   val actorProps = Props[URLPersistentActor]
}

class URLPersistentLookupActor extends BaseLookupActor {

  override val domain = URLPersistentLookupActor.domain

  override val delay_when_starting_actor = Some(Timeout(100, TimeUnit.MILLISECONDS))

  override val actorProps = URLPersistentLookupActor.actorProps

  override def aroundPreRestart(reason : scala.Throwable, message : scala.Option[scala.Any]): Unit = {
    // Read in state here!
  }

  // For testing purposes
  override def aroundPreStart(): Unit = {
    //import scala.concurrent.duration._
    //context.system.scheduler.scheduleOnce(100 millisecond, self, ShutDownTime("XYZ2", 100 millisecond))

  }

  //override def receive = super[BaseLookupActor].receive orElse {
  //override def receive = {
  def localReceive : Actor.Receive = {

    // Not used - Dummy
    case sum : SumStock => {
      System.out.println("SumStock received!!!" )
      val selection : akka.actor.ActorSelection = context.actorSelection("*")
      //var tot : Int = 0
      val res = selection.ask(GetStock("xx")).mapTo[Int]
      res.map{ i =>
        //tot = tot + i
        System.out.println("Sum1: " + i)
      }
      //tot = 0
      res.map{
        case i : Int => {
          //tot = tot + i
          System.out.println("Sum2: " + i)
          sender ! i
        }
      }
    }


  }


  override def receive = localReceive orElse super[BaseLookupActor].receive
}