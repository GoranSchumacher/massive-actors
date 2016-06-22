package actors

import actors.massive.base.LookupActorName
import akka.actor.{DeadLetter, Actor}
import akka.persistence.SaveSnapshotSuccess

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 02/06/2016
 */
class DeadLetterActor extends Actor {

  def receive = {
    case dead : DeadLetter if dead.message.isInstanceOf[LookupActorName] => {
      println(s"ECHOING to ${dead.recipient.path.parent} MESSAGE: ${dead.message}")
      context.actorSelection(dead.recipient.path.parent).tell(dead.message, dead.sender)
    }
    //case msg => println(s"DEAD LETTER RECEIVED FROM $sender Message: $msg")
  }

}
