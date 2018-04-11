package services

import controllers.cases.PushEvent
import core.PubSubClient
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import repositories.PushRepository
import repositories.models.GithubPush

import scala.concurrent.{ExecutionContext, Future}

case class PushRequest(subscriberId: Long, data: PushEvent)

@Singleton
class PushEventService @Inject()(pushRepository: PushRepository,
                                 pubSubClient: PubSubClient)(implicit ex: ExecutionContext) {
    def processPush(request: PushRequest): Future[String] = {
        createPush(request)
            .flatMap { id: Long =>
                Logger.debug(s"publishing subId: ${request.subscriberId}, pushId: $id")
                publishToPubSub(createPushPubSubMessage(request.subscriberId, id))
            } recoverWith {
            case e: Throwable =>
                Logger.error(s"error while publishing $request")
                Future.failed(new RuntimeException("publish error", e))
        }

    }

    private def createPush(request: PushRequest): Future[Long] = {
        pushRepository.insertPush(GithubPush(request.data.pusher.name,
            request.data.commits.length, request.subscriberId)) flatMap {
            case Some(insertId) => Future.successful(insertId)
            case None => Future.failed(new RuntimeException("push not inserted"))
        }
    }

    private def createPushPubSubMessage(subscriberId: Long, pushId: Long): String = {
        Json.stringify(Json.obj("subscriberId" -> subscriberId, "pushId" -> pushId))
    }

    private def publishToPubSub(message: String): Future[String] = {
        pubSubClient.publish("github", message)
    }
}