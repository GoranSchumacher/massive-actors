package actors.massive.stringtest

import java.util.concurrent.TimeUnit

import actors.massive.base.BaseLookupActor
import actors.massive.stock.{GetStock, SumStock}
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 01/02/2016
 */
object StringTestPersistentLookupActor extends BaseLookupActor{
  var actorRef : Option[ActorRef] = None

  override val domain = "StringTestPersistent"

  override val actorProps = Props[StringTestPersistentActor]
}

class StringTestPersistentLookupActor extends BaseLookupActor {

  override val domain = StringTestPersistentLookupActor.domain

  override val delay_when_starting_actor = Some(Timeout(100, TimeUnit.MILLISECONDS))

  override val actorProps = StringTestPersistentLookupActor.actorProps

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
  //orElse super[BaseLookupActor].receive


  override def receive = localReceive orElse super[BaseLookupActor].receive
}