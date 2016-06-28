package actors.massive.countmessages

import java.util.concurrent.TimeUnit

import actors.massive.base.{LookupActorName, LookupActorNameWithReply, BaseLookupActor}
import actors.massive.factorial.FactorialPersistentActor
import actors.massive.stock.{GetStock, SumStock}
import akka.actor.{Actor, Props}
import akka.util.Timeout

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 26/06/2016
 */
case class CountMess(name: String, count: Integer=0) extends LookupActorName {
  def incr: CountMess = {
    CountMess(this.name, this.count+1)
  }
}
case class CountMessAnswer(name: String) extends LookupActorNameWithReply
object CountMessagesPersistentLookupActor {

  //lazy val system = ActorSystem("example") // TODO This is not the same actorsystem as the Play actor system
  //lazy val lookupActor = system.actorOf(Props[URLPersistentLookupActor], "URLPersistentLookupActor")

  val TOPIC_ALL_SUBSCRIPTION = 0 // Length changed on url content
  val TOPIC_SUBSCRIPTION_LENGTH = 1 // Length changed on url content

  val domain = "CountMessages"

  //val actorProps = Props[FactorialPersistentActor]
}

class CountMessagesPersistentLookupActor extends BaseLookupActor {

  override val domain = CountMessagesPersistentLookupActor.domain

  override val delay_when_starting_actor = Some(Timeout(100, TimeUnit.MILLISECONDS))

  override val actorProps = Props(classOf[CountMessagesPersistentActor], context.self)

  override def aroundPreRestart(reason : scala.Throwable, message : scala.Option[scala.Any]): Unit = {
    // Read in state here!
  }

  // For testing purposes
  override def aroundPreStart(): Unit = {
    //import scala.concurrent.duration._
    //context.system.scheduler.scheduleOnce(100 millisecond, self, ShutDownTime("XYZ2", 100 millisecond))
  }

//  def localReceive : Actor.Receive = {
//
//    // Not used - Dummy
//    case sum : SumStock => {
//      System.out.println("SumStock received!!!" )
//      val selection : akka.actor.ActorSelection = context.actorSelection("*")
//      //var tot : Int = 0
//
//      import akka.pattern.ask
//      val res = selection.ask(GetStock("xx")).mapTo[Int]
//      res.map{ i =>
//        //tot = tot + i
//        System.out.println("Sum1: " + i)
//      }
//      //tot = 0
//      res.map{
//        case i : Int => {
//          //tot = tot + i
//          System.out.println("Sum2: " + i)
//          sender ! i
//        }
//      }
//    }
//
//
//  }


  //override def receive = localReceive orElse super[BaseLookupActor].receive
  override def receive = super[BaseLookupActor].receive
}