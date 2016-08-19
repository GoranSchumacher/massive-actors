package app

import actors.DeadLetterActor
import actors.massive.base.{ActorReference, GetActorRef}
import actors.massive.countmessages.{CountMess, CountMessAnswer, CountMessagesPersistentLookupActor}
import actors.massive.factorial.FactorialRequest
import akka.actor._
import akka.dispatch.UnboundedMailbox
import akka.util.Timeout
import util.Timer._

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 17/08/2016
 */
object DeadLetterLoopingTest extends App {

  import java.util.concurrent.TimeUnit

  implicit val timeout = Timeout(3000, TimeUnit.SECONDS)

  lazy val system = ActorSystem("example")

  /////////// Start Deadletter Watcher
  val deadLettersSubscriber = system.actorOf(Props[DeadLetterActor], name = "dead-letters-subscriber")
  system.eventStream.subscribe(deadLettersSubscriber, classOf[DeadLetter])
  Thread.sleep(2000)

  val CountMessagesPersistentLookupActor = system.actorOf(Props[CountMessagesPersistentLookupActor], name = "CountMessagesPersistentLookupActor")

  import scala.concurrent.duration._

  // This is just a dummy messages.
  // The actor do not care about message type, it just counts the number of messages
  val entityName = "dummyLoop4"
  var mess = CountMess(entityName)

  import akka.pattern.ask
  import scala.concurrent.ExecutionContext.Implicits.global
  val loopCount = 10000000

  (CountMessagesPersistentLookupActor ? GetActorRef(entityName)).map { case actorReference: ActorReference =>
    time(f"$loopCount%,15d calls") {
      for (step <- 1 to loopCount) {
        actorReference.actorRef ! mess.copy(count = step)
      }
      (actorReference.actorRef ? CountMessAnswer(entityName)).map { case i: Int => println(f"Answer should be +10: $i%,15d") }.
        onFailure{case ex: Exception => println(s"Future returned exception $ex")}
    }
  }

  Thread.sleep(5000000)
  system.terminate()
}

