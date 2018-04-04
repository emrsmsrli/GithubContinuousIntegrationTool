package services

import controllers.cases.PushEvent
import core.PubSubClient
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import repositories.PushRepository
import repositories.models.GithubPush

import scala.concurrent.{ExecutionContext, Future}

case class PushRequest(id: Int, data: PushEvent)
case class PushPubsubMessage(subId: Int, pushId: Int)

@Singleton
class PushService @Inject()(pushRepository: PushRepository,
                            pubSubClient: PubSubClient)(implicit ex: ExecutionContext) {

    def processPush(request: PushRequest): Future[String] = {
        pushRepository.insertPush(GithubPush(request.data.pusher.name,
            request.data.commits.length, request.id)).flatMap { id =>
            Logger.info(s"publishing subId: ${request.id}, pushId: $id")
            pubSubClient.publish("github", PushService.newMsg(PushPubsubMessage(request.id, id)))
        } recoverWith {
            case e =>
                Logger.error(s"error while publishing $request")
                Future.failed(new RuntimeException("publish error", e))
        }
    }
}

object PushService {
    implicit val ppsmw: Writes[PushPubsubMessage] = Json.writes[PushPubsubMessage]

    def newMsg(pushPubsubMessage: PushPubsubMessage): String = {
        Json.stringify(Json.toJson(pushPubsubMessage)(ppsmw))
    }
}