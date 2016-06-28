package actors

import actors.massive.base.LookupActorName
import actors.massive.countmessages.CountMess
import akka.actor.{DeadLetter, Actor}
import akka.persistence.SaveSnapshotSuccess
import akka.persistence.serialization.Message

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 02/06/2016
 */
class DeadLetterActor extends Actor {

  //TODO: Since an actorRef can be in the cache of the Lookup => Send a special VERIFY message to lookup.

  def receive = {
    case dead : DeadLetter if dead.message.isInstanceOf[LookupActorName] => {
      println(s"ECHOING to ${dead.recipient.path.parent} MESSAGE: ${dead.message} as Sender: ${dead.sender}")
      context.actorSelection(dead.recipient.path.parent).tell(dead.message, dead.sender)
    }
    case dead : DeadLetter if dead.message.isInstanceOf[CountMess] => {
      println(s"ECHOING CountMess to ${dead.recipient.path.parent} MESSAGE: ${dead.message} as Sender: ${dead.sender}")
      context.actorSelection(dead.recipient.path.parent).tell(dead.message, dead.sender)
    }

    case dead : DeadLetter =>
      println(s"NOT ECHOING DeadLetter to ${dead.recipient.path.parent} MESSAGE: ${dead.message}")



  }

}
