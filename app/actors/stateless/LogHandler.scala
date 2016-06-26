package actors.stateless

import akka.actor.Actor
import akka.event.Logging._
import com.sksamuel.elastic4s.ElasticClient

/**
 * Will handle sending log to ElasticSearch
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 25/06/2016
 */
class LogHandler extends Actor{

  lazy val esClient = ElasticClient.remote("127.0.0.1", 9300)

  def receive = {
    case InitializeLogger(_) =>
      sender() ! LoggerInitialized

    case e@ Error(cause, logSource, logClass, message) =>
      println(s"DEBUG: [$logSource - $logClass] $message ${e.mdc}")
      persistElasticsearch(e)

    case w@ Warning(logSource, logClass, message) =>
      println(s"DEBUG: [$logSource - $logClass] $message ${w.mdc}")
      persistElasticsearch(w)

    case i@ Info(logSource, logClass, message) =>
      println(s"DEBUG: [$logSource - $logClass] $message ${i.mdc}")
      persistElasticsearch(i)

    case d@ Debug(logSource, logClass, message) =>
      println(s"DEBUG: [$logSource - $logClass] $message ${d.mdc}")
      persistElasticsearch(d)

  }

  def persistElasticsearch(logEvent: LogEvent): Unit = {
    import com.sksamuel.elastic4s.ElasticDsl._
    import com.sksamuel.elastic4s.jackson.ElasticJackson
    import ElasticJackson.Implicits._
    esClient.execute {
      index into "actor" / "Log" source logEvent id logEvent.timestamp
    }
//      .map { t =>
//      log.debug(s"Persisted to Elastic: $t")
//    }.onFailure{
//      case t : Throwable => log.error(t, "Persisted to Elastic")
//    }
  }
}
