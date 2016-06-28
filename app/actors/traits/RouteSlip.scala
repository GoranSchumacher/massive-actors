package actors.traits

import akka.actor.{ActorContext, Props, ActorRef}

/**
 * From the book: "Akka in Action"
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 26/06/2016
 */
case class RouteSlipMessage(routeSlip: Seq[ActorRef],
                            message: AnyRef, forwardToSender: Boolean = false, originalSender: Option[ActorRef] = None)

trait RouteSlip {

  /**
   * Traverse to next actor in the queue
   * @param previousSender Warning: This should NOT be the sender of the message but the sender.sender!!
   * @param oldRouteSlipMessage
   * @param newMessage
   * @param context
   * @return
   */
  def sendMessageToNextTask(previousSender: ActorRef, oldRouteSlipMessage: RouteSlipMessage, newMessage: AnyRef)(implicit context: ActorContext) {
    var copySlip: RouteSlipMessage = oldRouteSlipMessage.copy(message = newMessage)

    if(copySlip.forwardToSender && copySlip.originalSender.isEmpty) {

      //lazy val ForwardToFutureActor = context.actorOf(Props[ForwardToFutureActor], "ForwardToFutureActor")
      copySlip = copySlip.copy(originalSender = Some(previousSender))
    }

    val nextTask = copySlip.routeSlip.head
    val newSlip = copySlip.routeSlip.tail
    copySlip = copySlip.copy(routeSlip = newSlip)

    if (newSlip.isEmpty) {
      if(copySlip.originalSender.isDefined) {
        nextTask.tell(copySlip.message, copySlip.originalSender.get)
      } else {
        nextTask ! copySlip
      }
    } else {
      nextTask ! copySlip
    }
  }
}