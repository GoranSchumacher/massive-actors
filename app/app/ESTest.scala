package app

import java.util.Date

import actors.massive.url.Url
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.jackson.ElasticJackson
import com.sksamuel.elastic4s.source.Indexable
import org.elasticsearch.common.settings.Settings

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 06/02/2016
 */
case class Url2(val name : String, url : String, repeat : Option[FiniteDuration]=None, data : String="", dataLength : Int=0, updated : Date= new Date())
object ESTest extends App {
  //val client = ElasticClient.local
  val client = ElasticClient.remote("127.0.0.1", 9300)

//  val settings = ImmutableSettings.settingsBuilder()
//    .put("http.enabled", false)
//    .put("path.home", "/var/elastic/")
//  val client = ElasticClient.local(settings.build())

//  val settings2 = Settings.builder()
//    .put("http.enabled", false)
//    .put("path.home", "/Users/GSConsulting/Desktop/ElasticSearch/elasticsearch-2.2.0")
//  val client = ElasticClient.local(settings2.build())

  //val client = ElasticClient.local

  // await is a helper method to make this operation synchronous instead of async
  // You would normally avoid doing this in a real program as it will block your thread
  client.execute { index into "bands" / "artists" fields "name"->"coldplay" }.await

  // how you turn the type into json is up to you
//  implicit object UrlIndexable extends Indexable[Url] {
//    override def json(t: Url): String = s""" { "name" : "${t.name}", "url" : "${t.url}" } """
//  }

  // Import this to be able do omit writing the above.
  import ElasticJackson.Implicits._

  // now the index request reads much cleaner
  val url = Url2("Microsoft", "ms.com")
  client.execute {
    index into "url" / "url" source url id url.name
  }.map{t => System.out.println(t)}

  // we need to wait until the index operation has been flushed by the server.
  // this is an important point - when the index future completes, that doesn't mean that the doc
  // is necessarily searchable. It simply means the server has processed your request and the doc is
  // queued to be flushed to the indexes. Elasticsearch is eventually consistent.
  // For this demo, we'll simply wait for 2 seconds (default refresh interval is 1 second).
  Thread.sleep(2000)

  // now we can search for the document we indexed earlier
  val resp = client.execute { search in "url" / "url" query "Microsoft" }.await
  println(resp)

}
