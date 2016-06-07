package sample.persistence

//#persistent-actor-example

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.persistence._

import scala.concurrent.duration.Duration

case class Cmd(data: String)
case class Evt(data: String)

case class ExampleState(events: List[String] = Nil) {
  def updated(evt: Evt): ExampleState = copy(evt.data :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}

class ExamplePersistentActor extends PersistentActor {
  override def persistenceId = self.path.toString

  var state = ExampleState()

  def updateState(event: Evt): Unit =
    state = state.updated(event)

  def numEvents =
    state.size

  val receiveRecover: Receive = {
    case evt: Evt =>
      println("Got Event: " + evt.toString)
      updateState(evt)
    case SnapshotOffer(metadata: SnapshotMetadata, snapshot: ExampleState) =>
      println("Got snapshot id: " + metadata.sequenceNr)
      state = snapshot
  }

  val receiveCommand: Receive = {
    case Cmd(data) =>
      persist(Evt(s"${data}-${numEvents +1}")) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
      }
      // Command Sourcing - According to doc
    //case Cmd(data) => persistAsync(data) (callback)

    case "snap"  => saveSnapshot(state)

    case "print" => println(state)

    case "setWakeupTimer" => {
      import scala.concurrent.duration._
      import scala.language.postfixOps
      import scala.concurrent.ExecutionContext.Implicits.global
      context.system.scheduler.scheduleOnce(15000 milliseconds, self, "wakeUp")
      println("setWakeupTimer called!")
      context.stop(self)
      //self ! PoisonPill
    }

    case "wakeUp" => {
      println("wakeUp called!")
      println(state)
    }
  }


  override def aroundPostStop() {
    println("aroundPostStop called!")
    saveSnapshot(state)
  }

}
//#persistent-actor-example

//object PersistentActorExample extends App {
//
//  val system = ActorSystem("example")
//  val persistentActor = system.actorOf(Props[ExamplePersistentActor], "persistentActor-4-scala")
//
//    persistentActor ! Cmd("foo")
//    persistentActor ! Cmd("baz")
//    persistentActor ! Cmd("bar")
//    persistentActor ! "snap"
//    persistentActor ! Cmd("buzz")
//    persistentActor ! "print"
//    persistentActor ! "setWakeupTimer"
//    persistentActor ! Cmd("goran")
//    persistentActor ! "print"
//    //persistentActor ! PoisonPill
//
//  Thread.sleep(30000)
//  //system.shutdown()
//  system.terminate()
//}
