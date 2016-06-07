package actors.massive.base

import akka.actor.Actor
import akka.persistence.{SnapshotMetadata, SnapshotOffer, PersistentActor}
import sample.persistence.ExampleState

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 01/02/2016
 */

abstract class Cmd()

abstract class Evt()

abstract class State[E]() {
  def updated(evt : E): State[E]
  //def size: Int = events.length
  def toString: String
}

abstract class BasePersistentAutoShutdownActor  extends  BaseAutoShutdownActor with PersistentActor{

  override def persistenceId = self.path.toString

  def receiveCommand : Receive

  override def receive : Receive = receiveCommand
  def receiveShutDown : Receive = super[BaseAutoShutdownActor].receive

}
