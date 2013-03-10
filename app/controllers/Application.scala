package controllers

import play.api._
import play.api.mvc._
import play.api.cache.Cached
import play.api.db.DB
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WS
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.i18n._
import models._
import scala.concurrent.Future

object Application extends Controller {

  val feedbackForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "feedback" -> nonEmptyText
    )(Feedback.apply)(Feedback.unapply)
  )


  def index = Action { implicit req =>
    DB.withConnection {
      implicit conn =>
        Ok(views.html.index(feedbackForm, Feedback.findAll, req.flash.get("status")))
    }
  }

  def leaveFeedback = Action { implicit req =>
    DB.withConnection {
      implicit conn =>
        val form = feedbackForm.bindFromRequest()
        form.fold(
        form => Ok(views.html.index(form, Feedback.findAll)), {
          feedback =>
            feedback.insert()
            channel.push(Json.toJson(feedback))
            Redirect(routes.Application.index())
              .flashing("status" -> Messages("feedback.left.by", feedback.username))
        }
        )
    }

  }

  val kloutKey = /* HIDDEN */                                                                      "insert your real klout key here"

  def influence(username: String) = Cached("influence." + username, 120)(Action {
    val future: Future[Result] = WS.url("http://api.klout.com/v2/identity.json/twitter").withQueryString(
      "screenName" -> username,
      "key" -> kloutKey
    ).get().flatMap { resp =>
      if (resp.status == 200) {
        val id = (resp.json \ "id").as[String]
        WS.url("http://api.klout.com/v2/user.json/" + id + "/score").withQueryString(
          "key" -> kloutKey
        ).get().map { resp =>
          Ok(resp.json)
        }
      } else {
        Future.successful(Ok(Json.obj("score" -> "0")))
      }
    }
    Async(future)
  })

  implicit val feedbackWrites = Json.writes[Feedback]

  val (broadcast, channel) = Concurrent.broadcast[JsValue]

  def feedbackFeed = WebSocket.using[JsValue] { req =>
    (Iteratee.foreach { json =>
      val form = feedbackForm.bind(json)
      if (!form.hasErrors) {
        DB.withConnection { implicit conn =>
          form.get.insert()
        }
        channel.push(Json.toJson(form.get))
      }
    }, broadcast)
  }

}
