package services

import com.google.inject.Inject
import dispatchers.NetworkDispatcher
import javax.inject.Singleton
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import repositories.models.GithubSubscriber

import scala.concurrent.Future

case class GithubHookResponse(id: Int, url: String)
case class GithubException(msg: String) extends Exception(msg)

@Singleton
class GithubRequestsService @Inject()(ws: WSClient)
                                     (implicit nd: NetworkDispatcher) {
    def registerWebhook(subscriber: GithubSubscriber)
                       (implicit ghrr: Reads[GithubHookResponse]): Future[Option[GithubHookResponse]] = {
        Logger.debug("registering webhook to github")
        val data = Json.obj(
            "name" -> "web",
            "config" -> Json.obj(
                "url" -> formatPubSubUrl(subscriber.id),
                "content_type" -> "json"
            )
        )

        ws.url(formatGithubHookUrl(subscriber.username, subscriber.repository))
            .addHttpHeaders("User-Agent" -> "WS", "Authorization" -> s"token ${subscriber.token}")
            .post(data)
            .map { response: WSResponse =>
                if(response.status != 201) {
                    Logger.error(s"github register create webhook failed. ${response.body}")
                    return Future.successful(None)
                }

                response.json.validate[GithubHookResponse] match {
                    case success: JsSuccess[GithubHookResponse] =>
                        Logger.debug(s"register webhook successful")
                        Some(success.get)
                    case err: JsError =>
                        Logger.error("github hook response parsing failed")
                        throw GithubException(s"hook response failed: ${err.errors.mkString}")
                }
            }
    }

    def downloadZipBall(subscriber: GithubSubscriber): Future[Array[Byte]] = {
        Logger.debug(s"downloading zip from ${subscriber.username}/${subscriber.repository}")
        ws.url(formatGithubZipballUrl(subscriber.username, subscriber.repository))
            .withFollowRedirects(true)
            .addHttpHeaders("Authorization" -> s"token ${subscriber.token}")
            .get()
            .map { response: WSResponse =>
                if(response.status != 200) {
                    Logger.error(s"download zipball failed ${response.body}")
                    throw GithubException("download zipball failed")
                }

                response.bodyAsBytes.toByteBuffer.array()
            }
    }
}
