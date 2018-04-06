package services

import com.google.inject.Inject
import dispatchers.NetworkDispatcher
import javax.inject.Singleton
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.Implicits.{implicitGer, implicitGhr}
import repositories.models.GithubSubscriber

import scala.concurrent.Future

case class GithubHookResponse(id: Int, url: String)

case class GithubErrorResponse(message: String)

@Singleton
class GithubService @Inject()(ws: WSClient)
                             (implicit nd: NetworkDispatcher) {
    def registerWebhook(subscriber: GithubSubscriber) : Future[Option[GithubHookResponse]] = {

        Logger.debug("registering webhook to github")
        val data = Json.obj(
            "name" -> "web",
            "config" -> Json.obj(
                "url" -> formatPubSubUrl(subscriber.id),
                "content_type" -> "json"
            )
        )

        // TODO make this parsing cuter
        ws.url(formatGithubHookUrl(subscriber.username, subscriber.repository))
            .addHttpHeaders("User-Agent" -> "WS", "Authorization" -> s"token ${subscriber.token}")
            .post(data)
            .map { response: WSResponse =>
                response.json.validate[GithubHookResponse](implicitGhr) match {
                    case success: JsSuccess[GithubHookResponse] =>
                        Logger.debug(s"register webhook successful")
                        Some(success.get)
                    case _: JsError =>
                        Logger.error("github hook response parsing failed, falling back to error parse")
                        response.json.validate[GithubErrorResponse](implicitGer) match {
                            case success: JsSuccess[GithubErrorResponse] =>
                                Logger.error(s"webhook register unsuccessful: ${success.get.message}")
                            case failure: JsError =>
                                Logger.error(s"github error respons parsing failed: ${failure.errors
                                    .addString(new StringBuilder())}")
                        }
                        None
                }
            }
    }
}
