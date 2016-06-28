package app

import actors.DeadLetterActor
import actors.massive.base.{ActorReference, GetActorRef, ShutDownTime}
import actors.massive.countmessages.{CountMessAnswer, CountMess, CountMessagesPersistentLookupActor}
import akka.actor._
import akka.util.Timeout

/**
 * @see https://danielasfregola.com/2015/05/04/akka-dead-letters-channel/
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 02/06/2016
 */
object DeadLetterTest extends App {

  import java.util.concurrent.TimeUnit

  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  lazy val system = ActorSystem("example")

  /////////// Start Deadletter Watcher
  val deadLettersSubscriber = system.actorOf(Props[DeadLetterActor], name = "dead-letters-subscriber")
  system.eventStream.subscribe(deadLettersSubscriber, classOf[DeadLetter])
  Thread.sleep(2000)

  val echoActor = system.actorOf(Props[DeadLetterActor], name = "generic-echo-actor")
  echoActor ! "First Message"
  // generic-echo-actor - New msg received: First Message

  echoActor ! PoisonPill
  echoActor ! "Second Message"
  // dead-letters-subscriber - New msg received: DeadLetter(Second Message,Actor[akka://dead-letters-usage-example/deadLetters],Actor[akka://dead-letters-usage-example/user/generic-echo-actor#317003256])
  // INFO  [RepointableActorRef]: Message [java.lang.String] from Actor[akka://dead-letters-usage-example/deadLetters] to Actor[akka://dead-letters-usage-example/user/generic-echo-actor#317003256] was not delivered. [1] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.

  system.deadLetters ! "Dead Message"
  // dead-letters-subscriber - New msg received: DeadLetter(Dead Message,Actor[akka://dead-letters-usage-example/deadLetters],Actor[akka://dead-letters-usage-example/deadLetters])
  // INFO  [DeadLetterActorRef]: Message [java.lang.String] from Actor[akka://dead-letters-usage-example/deadLetters] to Actor[akka://dead-letters-usage-example/deadLetters] was not delivered. [2] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.

  val CountMessagesPersistentLookupActor = system.actorOf(Props[CountMessagesPersistentLookupActor], name = "CountMessagesPersistentLookupActor")

  import scala.concurrent.duration._

  // This is just a dummy messages.
  // The actor do not care about message type, it just counts the number of messages
  val entityName = "dummy2"
  var mess = CountMess(entityName)
  CountMessagesPersistentLookupActor ! mess //0
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //1
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //2
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //3
  mess = mess.incr

  import akka.pattern.ask
  import scala.concurrent.ExecutionContext.Implicits.global

  (CountMessagesPersistentLookupActor ? GetActorRef(entityName)).map { case a: ActorReference => {
    // Here the actor will die
    a.actorRef ! PoisonPill
  }
  }
  Thread.sleep(5000)
  // But be revived on the next message
  CountMessagesPersistentLookupActor ! mess //4
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //5
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //6
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //7
  mess = mess.incr
  (CountMessagesPersistentLookupActor ? GetActorRef(entityName)).map { case a: ActorReference => {
    a.actorRef ! mess //10
  }
  }
  CountMessagesPersistentLookupActor ! mess //8
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //9
  mess = mess.incr

  import akka.pattern.ask
  import scala.concurrent.ExecutionContext.Implicits.global

  (CountMessagesPersistentLookupActor ? CountMessAnswer(entityName)).map { a => println(s"Answer should be 10: $a") }

  Thread.sleep(5000)
  system.terminate()
}
