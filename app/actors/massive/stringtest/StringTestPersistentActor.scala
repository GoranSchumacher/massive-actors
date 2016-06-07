package actors.massive.stringtest

import actors.massive.base._
import akka.persistence.{SnapshotMetadata, SnapshotOffer}

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 01/02/2016
 */
//case class Cmd2(name : String, data: String)
case class Cmd(override val name : String, data : String) extends LookupActorName
case class Print(override val name : String) extends LookupActorName

case class Evt2(data: String)
  //extends actors.massive.base.Evt

case class State2(events: List[String] = Nil) {
  def updated(evt: Evt2): State2 = copy(evt.data :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}

class StringTestPersistentActor extends BasePersistentAutoShutdownActor {

  override var domain = StringTestPersistentLookupActor.domain

  var state : State2 = State2()

  def updateState(event: Evt2): Unit =
    state = state.updated(event)

  override def aroundPreStart: Unit = {
    import scala.concurrent.duration._
    self ! ShutDownTime("Self", 30 seconds)
  }

  override def aroundPostStop(): Unit = {
    saveSnapshot(state)
  }

  def receiveRecover: Receive = {
    case evt: Evt2 =>
      log.debug("Got Event: " + evt.toString)
      updateState(evt)
    case SnapshotOffer(metadata: SnapshotMetadata, snapshot: State2) =>
      log.debug("Got snapshot id: " + metadata.sequenceNr)
      state = snapshot
  }

  def numEvents =
    state.asInstanceOf[State2].size

  override val receiveCommand: Receive = super[BasePersistentAutoShutdownActor].receiveShutDown orElse {
    case Cmd(name, data) =>
      System.out.println(s"Cmd: name:  $name data: $data")
      //persist(Evt2(s"${data}-${numEvents +1}")) { event =>
      val event = Evt2(s"${data}-${numEvents +1}")
        updateState(event)
        //context.system.eventStream.publish(event)
      //}
    // Command Sourcing - According to doc
    //case Cmd(data) => persistAsync(data) (callback)

    case "snap"  => saveSnapshot(state)

    case Print(name) => {
      System.out.println(s"Print State: $state")
      log.debug(s"Print State: $state")
    }

    case "setWakeupTimer" => {
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.duration._
      import scala.language.postfixOps
      context.system.scheduler.scheduleOnce(2000 milliseconds, self, "wakeUp")
      log.debug("setWakeupTimer called!")
      context.stop(self)
      //self ! PoisonPill
    }

    case "wakeUp" => {
      log.debug("wakeUp called!")
      log.debug(s"State: $state")
    }

    case mess  => System.out.println(s"(URLPersistentActor): MESSAGE NOT MATCHED: $mess Sender: $sender")
  }
}
