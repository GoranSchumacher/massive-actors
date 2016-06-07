package actors

import akka.actor.Actor

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 02/06/2016
 */
class EchoActor extends Actor {

  def receive = {
    case msg => println(s"New msg received: $msg")
  }

}
