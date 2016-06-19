package actors.massive.url

import java.util.Date

import actors.UserSocket.ChatMessage
import actors.massive.base._
import actors.massive.web.MyWebSocketActor
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.persistence.{SnapshotMetadata, SnapshotOffer}
import app.Url2
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.index
import com.sksamuel.elastic4s.jackson.ElasticJackson
import controllers.InEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 01/02/2016
 */
case class Url(override val name : String, url : String, repeat : Option[FiniteDuration]=None, data : String="", dataLength : Int=0, updated : Option[Date]=None) extends LookupActorName
case class Print(override val name : String) extends LookupActorName

case class MyState(event: Url) {
  def updated(evt: Url): MyState = MyState(evt)
  override def toString: String = event.toString
}

class URLPersistentActor extends BasePersistentAutoShutdownActor {

  lazy val esClient = ElasticClient.remote("127.0.0.1", 9300)

  override var domain = URLPersistentLookupActor.domain

  var state : MyState = MyState(Url("", ""))

  override val topic = "chat"

  var notifyEnabled = false

  def updateState(event: Url): Unit = {
    val content = scala.io.Source.fromURL(event.url).mkString("")
    val updatedEvent = event.copy(data=content, dataLength=content.size, updated=Some(new Date()))

    // This is the way to use Akkas broadcast message, sending message through a topic
    // It is not used by MyWebsocketActor
    if(updatedEvent.dataLength != state.event.dataLength) {
      // Only called if the length has changed.
      mediator ! Publish(topic, ChatMessage("1", s"updateState called for actor ${context.self.path.name}, Value different: ${updatedEvent.dataLength}"))
    }
    mediator ! Publish(topic, ChatMessage("1", s"updateState called for actor ${context.self.path.name}, Value: ${updatedEvent.dataLength}"))

    // Here we set the timer for driving the repetiotion.
    if(!notifyEnabled) {
      System.out.println(s"schedule: repeat:  ${updatedEvent.repeat.get} URL: ${updatedEvent.url}")
      context.system.scheduler.schedule(updatedEvent.repeat.get, updatedEvent.repeat.get, self, updatedEvent)
      notifyEnabled = true
    }
    // Here we notify listening actors
    notifySubscribers(URLPersistentLookupActor.TOPIC_SUBSCRIPTION_LENGTH, (s"Notify. Url: ${updatedEvent.url}, Size: ${updatedEvent.dataLength} OldSize: ${state.event.dataLength}"))

    // Here we set the new state
    state = MyState(updatedEvent)

    // Persist url and content to Elastic search
    persistElasticsearch()
  }

  def persistElasticsearch(): Unit = {
    import com.sksamuel.elastic4s.ElasticDsl._
    import com.sksamuel.elastic4s.jackson.ElasticJackson
    import ElasticJackson.Implicits._
    esClient.execute {
      index into "actor" / "URLPersistentActor" source state id state.event.name
    }.map { t =>
      log.debug("Persisted to Elastic: $t")
    }
  }

  override def aroundPreStart: Unit = {
    notifyEnabled = false
  }

  override def aroundPostStop(): Unit = {
    saveSnapshot(state)
  }

  def receiveRecover: Receive = {
    case evt: Url =>
      log.debug("Got Event: " + evt.toString)
      updateState(evt)
    case SnapshotOffer(metadata: SnapshotMetadata, snapshot: MyState) =>
      log.debug("Got snapshot id: " + metadata.sequenceNr)
      state = snapshot
  }

  override val receiveCommand: Receive = super[BasePersistentAutoShutdownActor].receiveShutDown orElse {
    // Cmd and Event are the same class in the example
    case url : Url =>
      System.out.println(s"Cmd: name:  $url.name data: ${url.repeat}")
      persist(url) { event =>
        updateState(event)
      }

    case Print(name) => {
      System.out.println(s"Print State: $state")
      log.debug(s"Print State: $state")
    }

    case mess  => System.out.println(s"(URLPersistentActor): MESSAGE NOT MATCHED: $mess Sender: $sender")
  }
}
