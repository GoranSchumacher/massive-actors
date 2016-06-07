package actors.massive.stock

import java.util.concurrent.TimeUnit

import actors.UserSocket.ChatMessage
import actors.massive.base.{BaseAutoShutdownActor, LookupActorName, LookupActorNameWithReply}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.util.Timeout
import org.joda.time.DateTimeUtils

import scala.language.postfixOps

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 27/12/2015
 */

case class AddStock(override val name : String, number : Int) extends LookupActorName

case class GetStock(override val name : String) extends LookupActorNameWithReply
case class SumStock(override val name : String=null) extends LookupActorNameWithReply

class StockActor extends BaseAutoShutdownActor {

  //override val log = Logging(context.system, this)

  override var domain = StockLookupActor.domain

  var number : Int = 0

  //mediator ! Subscribe(topic, self)

  override def aroundPreStart: Unit = {

    log.debug("Starting StockActor")
    // Read in state here!
//    val content = scala.io.Source.fromURL("http://apple.com").mkString("")
//    System.out.println(content)
  }

  override def aroundPreRestart(reason : scala.Throwable, message : scala.Option[scala.Any]): Unit = {
    // Read in state here!
  }

  override def receive = super[BaseAutoShutdownActor].receive orElse {

    case add : AddStock =>
      number += add.number
      System.out.println("Addstock: " + add.name + " New number: " + number)
      lastMessageTSMillis = DateTimeUtils.currentTimeMillis()
    case get : GetStock =>
      mediator ! Publish(topic, ChatMessage("1", s"GetStock called for actor $self.name, Value: $number"))
      implicit val timeout = Timeout(5, TimeUnit.MINUTES)
      System.out.println(s"GetStock: $get.name New number:  $number")
      System.out.println(s"GetStock: $get.name Sender: $sender")
      sender ! number
      lastMessageTSMillis = DateTimeUtils.currentTimeMillis()

    case mess => System.out.println(s"(StockActor): MESSAGE NOT MATCHED: $mess Sender: $sender")
  }
}
