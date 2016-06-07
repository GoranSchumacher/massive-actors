package actors.massive.stock

import java.util.concurrent.TimeUnit

import actors.massive.base.{ShutDownTime, BaseLookupActor}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 27/12/2015
 */

object StockLookupActor {

  lazy val system = ActorSystem("example")
  lazy val actorRef = system.actorOf(Props[StockLookupActor], "StockLookupActor")

  //override
  val domain = "StockLookup"

  //override
  val actorProps = Props[StockActor]
}

class StockLookupActor extends BaseLookupActor {

  override val domain = StockLookupActor.domain

  override val actorProps = StockLookupActor.actorProps

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