package actors

import actors.massive.base.LookupActorName
import akka.actor.{Actor, DeadLetter}

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 02/06/2016
 */
class DeadLetterActor extends Actor {

  def receive = {
    case dead : DeadLetter if dead.message.isInstanceOf[LookupActorName] => {
      println(s"ECHOING to ${dead.recipient.path.parent} MESSAGE: ${dead.message} as Sender: ${dead.sender}")
      context.actorSelection(dead.recipient.path.parent).tell(dead.message, dead.sender)
    }

    case dead : DeadLetter =>
      println(s"NOT ECHOING DeadLetter to ${dead.recipient.path.parent} MESSAGE: ${dead.message}")



  }

}
