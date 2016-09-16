package controllers

import play.api.libs.json.{Json, JsArray}
import play.api.mvc.{Action, Controller}

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 21/08/2016
 */
class ReactController extends Controller {

  val JSON_KEY_AUTHOR = "author"
  val JSON_KEY_TEXT = "text"

  // Initialise the comments list
  var commentsJson: JsArray = Json.arr(
    Json.obj(JSON_KEY_AUTHOR -> "Pete Hunt", JSON_KEY_TEXT -> "This is one comment"),
    Json.obj(JSON_KEY_AUTHOR -> "Jordan Walke", JSON_KEY_TEXT -> "This is *another* comment")
  )

  def reactIndex = Action { implicit request =>
    Ok(views.html.react_url_actor())
  }

  // Returns the comments list
  def comments = Action {
    Ok(commentsJson)
  }

  // Adds a new comment to the list and returns it
  def comment(author: String, text: String) = Action {
    val newComment = Json.obj(
      JSON_KEY_AUTHOR -> author,
      JSON_KEY_TEXT -> text)
    commentsJson = commentsJson :+ newComment
    Ok(newComment)
  }
}
