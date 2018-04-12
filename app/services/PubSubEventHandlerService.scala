package services

import java.util.Date

import core.GStorageClient
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import repositories.{PushRepository, SubscriberRepository}
import repositories.models.{GithubPush, GithubSubscriber}

import scala.concurrent.{ExecutionContext, Future}

case class SubscriberPushIds(subscriberId: Long, pushId: Long)
case class PubSubEventException(msg: String) extends Exception(msg)

@Singleton
class PubSubEventHandlerService @Inject()(subscriberRepository: SubscriberRepository,
                                          pushRepository: PushRepository,
                                          gStorageClient: GStorageClient,
                                          githubRequestsService: GithubRequestsService)
                                         (implicit ec: ExecutionContext) {
    def processEvent(event: String)(implicit spir: Reads[SubscriberPushIds]): Future[Unit] = {
        Logger.debug(s"processing event $event")

        val ids = extractIds(event)
        for {
            subscriber <- getSubscriberInfo(ids.subscriberId)
            zip <- downloadZip(subscriber)
            uploadUrl <- uploadZip(subscriber, zip)
            updated <- updatePush(subscriber, ids, uploadUrl)
        } yield updated
    }

    def extractIds(event: String)(implicit spir: Reads[SubscriberPushIds]): SubscriberPushIds = {
        Json.parse(event).validate[SubscriberPushIds] match {
            case idData: JsSuccess[SubscriberPushIds] =>
                Logger.debug("json parse successful")
                idData.get
            case err: JsError =>
                Logger.error(s"process event parse json failed: ${err.errors.mkString(",")}")
                throw PubSubEventException(s"process event parse json failed: ${err.errors.mkString(",")}")
        }
    }

    def getSubscriberInfo(subscriberId: Long): Future[GithubSubscriber] = {
        subscriberRepository.getSubscriber(subscriberId) map {
            case Some(subscriber) => subscriber
            case None =>
                Logger.error("subscriber info not retrieved")
                throw PubSubEventException("subscriber info not retrieved")
        }
    }

    def downloadZip(subscriber: GithubSubscriber): Future[Array[Byte]] = {
        githubRequestsService.downloadZipBall(subscriber)
    }

    def uploadZip(subscriber: GithubSubscriber, data: Array[Byte]): Future[String] = {
        val fileName = s"${subscriber.username}_${subscriber.repository}_${new Date().getTime}.zip"
        Logger.debug(s"generated file name is $fileName")
        Logger.debug(s"zip downloaded, size ${data.length}")
        gStorageClient.upload(fileName, "linovi", data)
    }

    def updatePush(subscriber: GithubSubscriber, ids: SubscriberPushIds, remoteUrl: String): Future[Boolean] = {
        Logger.debug("zip uploaded")
        pushRepository.updatePush(GithubPush(subscriber.username, 0,
            ids.subscriberId, "DONE", ids.pushId, remoteUrl))
    }
}
