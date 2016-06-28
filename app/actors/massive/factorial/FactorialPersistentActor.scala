package actors.massive.factorial

import java.util.Date

import actors.UserSocket.ChatMessage
import actors.massive.base._
import actors.massive.factorial.FactorialResponse
import akka.actor.ActorRef
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.persistence.{SnapshotMetadata, SnapshotOffer}
import com.sksamuel.elastic4s.ElasticClient
import org.joda.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 01/02/2016
 */
case class FactorialRequest(override val name : String) extends LookupActorName
case class FactorialResponse(override val name : String, result : Option[Long]) extends LookupActorName

case class MyState(result: FactorialResponse) {
  def request(request: FactorialRequest): MyState = MyState(FactorialResponse(request.name, None))
  def response(response: FactorialResponse): MyState = MyState(response)
  override def toString: String = result.toString
}

class FactorialPersistentActor(lookupActor : ActorRef) extends BasePersistentAutoShutdownActor {

  override var domain = FactorialPersistentLookupActor.domain

  var state : MyState = MyState(FactorialResponse("", None))

  override val topic = "chat"

  var originalSender : ActorRef = _

  def updateState(request: FactorialRequest): Unit = {
    originalSender = sender()
    // Here we set the new state
    state = MyState(FactorialResponse(request.name, None))
    val fact = Integer.parseInt(request.name)

    if (fact == 1) {
      originalSender ! FactorialResponse("1", Some(1))
    } else {
      lookupActor ! FactorialRequest((fact - 1).toString)
    }
  }

  def updateState(response: FactorialResponse): Unit = {
    //System.out.println(s"updateState: Response name:  $response")
    val x = Integer.parseInt(state.result.name)
    val res = response.result.map(_*x)
    val newResp = FactorialResponse(state.result.name, res)
    state = MyState(newResp)
    originalSender ! newResp
  }


  override def aroundPreStart: Unit = {
    self ! ShutDownTime(context.self.path.name, 2 minutes )
  }

  override def aroundPostStop(): Unit = {
    saveSnapshot(state)
  }

  def receiveRecover: Receive = {
    case request: FactorialRequest =>
      //log.debug("Got Event: " + request.toString)
      updateState(request)
    case response: FactorialResponse =>
      //log.debug("Got Event: " + response.toString)
      updateState(response)
    case SnapshotOffer(metadata: SnapshotMetadata, snapshot: MyState) =>
      //log.debug("Got snapshot id: " + metadata.sequenceNr)
      state = snapshot
  }

  override val receiveCommand: Receive = super[BasePersistentAutoShutdownActor].receiveShutDown orElse {
    // Cmd and Event are the same class in the example
    case request : FactorialRequest =>
      messageHandled()
      if(state.result.result.isDefined) {
        // We already have a cached result => return it and do not persist this message.
        sender() ! state.result
      } else {
        //System.out.println(s"Cmd: Request name:  $request.name")
        persist(request) { event =>
          updateState(event)
        }
      }
    case response : FactorialResponse =>
      messageHandled()
      //System.out.println(s"Cmd: Response name:  $response.name")
      persist(response) { event =>
        updateState(event)
      }

    case mess  => System.out.println(s"(FactorialPersistentActor): MESSAGE NOT MATCHED: $mess Sender: $sender")
  }
}
