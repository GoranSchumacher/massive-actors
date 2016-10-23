package app

import actors.DeadLetterActor
import actors.massive.base.{ActorReference, GetActorRef}
import actors.massive.countmessages.{CountMessAnswer, CountMess, CountMessagesPersistentLookupActor}
import akka.actor._
import akka.util.Timeout

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 23/10/2016
 */
object PoisonPillTest extends App {

  import java.util.concurrent.TimeUnit

  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  lazy val system = ActorSystem("example")

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

  Thread.sleep(500000)
  system.terminate()
}
