package actors.massive.base

import akka.actor.ActorRef
import akka.persistence.{SnapshotMetadata, SnapshotOffer}

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 04/03/2016
 */

case class SubscriberState(subscribers: scala.collection.mutable.Map[ActorRef, Subscribe] = scala.collection.mutable.Map[ActorRef, Subscribe]()) {
  def add(subscriber: Subscribe) = subscribers.put(subscriber.subscriber, subscriber)
  def remove(unSubscriber: UnSubscribe) = subscribers.remove(unSubscriber.subscriber)
    def hasSubscribers = {
      !subscribers.isEmpty
    }
  override def toString: String = subscribers.toString
}


class SubscriptionActor extends BasePersistentAutoShutdownActor {

  var state : SubscriberState = SubscriberState()

  override var domain : String = "Empty"

  def receiveRecover: Receive = {
    case subscribe : Subscribe =>
      log.debug("Got Event: " + subscribe.toString)
      state.add(subscribe)
    case unSubscribe : UnSubscribe =>
      log.debug("Got Event: " + unSubscribe.toString)
      state.remove(unSubscribe)
    case SnapshotOffer(metadata: SnapshotMetadata, snapshot: SubscriberState) =>
      log.debug("Got snapshot id: " + metadata.sequenceNr)
      state = snapshot
  }


  override val receiveCommand: Receive = super[BasePersistentAutoShutdownActor].receiveShutDown orElse {

    case subscribe : Subscribe => {
      System.out.println(s"Subscribe:  subscriber: ${subscribe.subscriber}")
      persist(subscribe) { event =>
        state.add(subscribe)
      }
    }

    case unSubscribe : UnSubscribe => {
      System.out.println(s"UnSubscribe:  subscriber: ${unSubscribe.subscriber}")
      persist(unSubscribe) { event =>
        state.remove(unSubscribe)
      }
    }
  }

  override def notifySubscribers(topic : Int, message : Any) = {
    System.out.println(s"In notifySubscribers ${state.subscribers}")
    state.subscribers.values.foreach{ s =>
      System.out.println(s"notifySubscribers:  subscriber: ${s.subscriber}")
      s.subscriber ! message
    }
    System.out.println(s"Out notifySubscribers")
  }
}
