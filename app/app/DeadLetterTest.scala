package app

import actors.DeadLetterActor
import actors.massive.base.{ActorReference, GetActorRef}
import actors.massive.countmessages.{CountMess, CountMessAnswer, CountMessagesPersistentLookupActor}
import actors.massive.factorial.FactorialRequest
import akka.actor._
import akka.util.Timeout
import util.Timer._

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
  val entityName = "dummy6"
  var mess = CountMess(entityName)
  CountMessagesPersistentLookupActor ! mess //0
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //1
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //2
  mess = mess.incr
  CountMessagesPersistentLookupActor ! mess //3
  mess = mess.incr


//  time("1.000.000 calls") {
//    for (step <- 1 to 1000000) {
//      CountMessagesPersistentLookupActor ! mess //10
//    }
//  }

//  import akka.pattern.ask
//  import scala.concurrent.ExecutionContext.Implicits.global
//  (CountMessagesPersistentLookupActor ? GetActorRef(entityName)).map { case a: ActorReference =>
//    time("1.000.000 calls") {
//      for (step <- 1 to 1000000) {
//        a.actorRef ! mess //10
//      }
//    }
//  }

  import akka.pattern.ask

  import scala.concurrent.ExecutionContext.Implicits.global

  var aRef: ActorRef = _
  (CountMessagesPersistentLookupActor ? GetActorRef(entityName)).map { case a: ActorReference =>
    // Here the actor will die
    a.actorRef ! PoisonPill
    aRef = a.actorRef
  }
  Thread.sleep(2000)
  // But be revived on the next message
  CountMessagesPersistentLookupActor ! mess //4   MISSED!!!!
  Thread.sleep(2000)
  mess = mess.incr
  // Here we send a message in the future to the dead actorRef - but miraculously it will arrive!!!
  system.scheduler.scheduleOnce(100 milliseconds, aRef, mess)
  //aRef ! mess //5
  mess = mess.incr
  // Here we send a message in the future to the dead actorRef - but miraculously it will arrive!!!
  system.scheduler.scheduleOnce(150 milliseconds, aRef, mess)
  //aRef ! mess //6
  mess = mess.incr
  // Here we send a message to the dead actorRef - but miraculously it will arrive!!!
  aRef ! mess //7
  mess = mess.incr
  (CountMessagesPersistentLookupActor ? GetActorRef(entityName)).map { case a: ActorReference =>
    a.actorRef ! mess //8   MISSED!!!!
  }
  mess = mess.incr
  aRef ! mess //9
  mess = mess.incr



  import akka.pattern.ask
  import scala.concurrent.ExecutionContext.Implicits.global
  Thread.sleep(2000)
  (CountMessagesPersistentLookupActor ? CountMessAnswer(entityName)).map { a => println(s"Answer should be +10: $a") }

  Thread.sleep(500000)
  system.terminate()
}
