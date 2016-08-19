package actors.massive.countmessages

import actors.massive.base.{BasePersistentAutoShutdownActor, ShutDownTime}
import akka.actor.ActorRef
import akka.event.Logging._
import akka.persistence.{SnapshotMetadata, SnapshotOffer}

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 26/06/2016
 */
class CountMessagesPersistentActor(lookupActor : ActorRef) extends BasePersistentAutoShutdownActor {

  override var domain = CountMessagesPersistentLookupActor.domain

  var state : Integer = 0

  override val topic = "chat"

  var originalSender : ActorRef = _

  def updateState(): Unit = {
    originalSender = sender()
    // Here we set the new state
    state = state + 1
  }

  override def aroundPreStart: Unit = {
    import scala.concurrent.duration._
    self ! ShutDownTime(context.self.path.name, 5 seconds )
  }

  override def aroundPostStop(): Unit = {
    saveSnapshot(state)
  }

  def receiveRecover: Receive = {
    case mess: CountMess =>
      updateState()
      println(s"receiveRecover Message Received: ${state}")
    case SnapshotOffer(metadata: SnapshotMetadata, snapshot: Integer) =>
      state = snapshot
      println(s"receiveRecover Snapshot Received: ${state}")
  }

  override val receiveCommand: Receive = super[BasePersistentAutoShutdownActor].receiveShutDown orElse {
    // Cmd and Event are the same class in the example
    case mess: CountMess  =>
      // Another way of adding log key/values
      val mdc = Map("countMess.count2" -> mess.count)

      log.mdc(mdc)
      log.debug(s"Received message $mess") // By some reason, isErrorEnabled is false
      log.error(s"Received message $mess") // By some reason, isErrorEnabled is false

      // Make a snapshot every 100.000 message, for performance.
      // When making snapshot, you must have made a persistence call first.
      if (countMessagesSinceSnapshot==0) {
        persist(mess) { event =>
          updateState()
        }
      } else {
        updateState()
      }
      messageHandled()
      if(countMessagesSinceSnapshot >100000) {
        saveSnapshot(state)
        countMessagesSinceSnapshot=1
      }

    case mess: CountMessAnswer =>
      println(s"CountMessAnswer Received from sender: $sender!!!")
      messageHandled()
      sender() ! state

    case mess  => System.out.println(s"(CountMessagesPersistentActor): MESSAGE NOT MATCHED: $mess Sender: $sender")
  }

  // Add key/values to logging
  var reqId = 0
  override def mdc(currentMessage: Any): MDC = {
    reqId += 1
    val always = Map("requestId" -> reqId)
    val perMessage = currentMessage match {
      case countMess: CountMess => Map("countMess.count" -> countMess.count)
      case _      => Map()
    }
    always ++ perMessage
  }
}