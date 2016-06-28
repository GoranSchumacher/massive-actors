package actors.massive.countmessages

import actors.massive.base.{BasePersistentAutoShutdownActor, ShutDownTime}
import akka.actor.ActorRef
import akka.persistence.{SnapshotMetadata, SnapshotOffer}
import akka.persistence.serialization.Message

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
      println(s"Mess Received: $mess")
      messageHandled()
      persist(mess) { event =>
        updateState()
      }

    case mess: CountMessAnswer =>
      messageHandled()
      sender() ! state

    case mess  => System.out.println(s"(CountMessagesPersistentActor): MESSAGE NOT MATCHED: $mess Sender: $sender")
  }
}