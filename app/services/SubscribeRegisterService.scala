package services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import repositories.SubscriberRepository
import repositories.models.GithubSubscriber
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

case class SubscribeRegisterException(msg: String, t: Throwable)
    extends Exception(msg, t)

@Singleton
class SubscribeRegisterService @Inject()(subscriberRepository: SubscriberRepository,
                                         githubRequestsService: GithubRequestsService)
                                        (implicit ec: ExecutionContext) {
    def subscribe(subscriber: GithubSubscriber): Future[Boolean] = {
        checkSubscriberAlreadyExists(subscriber).flatMap { doesExist: Boolean =>
            createSubscriberIfNotExists(subscriber, doesExist)
        }.flatMap { maybeInsertedId: Option[Long] =>
            registerWebhookIfCreated(subscriber, maybeInsertedId)
        }.flatMap { maybeResponse: Option[GithubHookResponse] =>
            updateSubscriberIfRegistered(subscriber, maybeResponse)
        } recoverWith {
            case t: Throwable =>
                Logger.error(s"error occurred while subscribing $t")
                deleteSubscriberIfError(subscriber, t)
        }
    }

    private def checkSubscriberAlreadyExists(subscriber: GithubSubscriber): Future[Boolean] = {
        Logger.debug(s"checking if subscriber already exists: $subscriber")
        subscriberRepository.getSubscriber(subscriber.username, subscriber.repository) map {
            case Some(_) => true
            case None => false
        }
    }

    private def createSubscriberIfNotExists(subscriber: GithubSubscriber,
                                            doesExist: Boolean): Future[Option[Long]] = {
        if(!doesExist) {
            val err = "subscriber already exists"
            Logger.error(err)
            throw SubscribeRegisterException(err, null)
        }
        subscriberRepository.insertSubscriber(subscriber)
    }

    private def registerWebhookIfCreated(subscriber: GithubSubscriber, maybeInsertId: Option[Long])
            : Future[Option[GithubHookResponse]] = {
        maybeInsertId match {
            case Some(insertId) =>
                githubRequestsService.registerWebhook(subscriber.copy(id = insertId))
            case None =>
                val err = "could not insert subscriber"
                Logger.error(err)
                throw SubscribeRegisterException(err, null)
        }
    }

    private def updateSubscriberIfRegistered(subscriber: GithubSubscriber,
                                             maybeResponse: Option[GithubHookResponse]): Future[Boolean] = {
        maybeResponse match {
            case Some(subscribed) =>
                subscriberRepository.updateSubscriber(subscriber.copy(webhookUrl = subscribed.url))
            case None =>
                throw SubscribeRegisterException("could not register webhook", null)
        }
    }

    private def deleteSubscriberIfError(subscriber: GithubSubscriber,
                                        t: Throwable): Future[Boolean] = {
        subscriberRepository.deleteSubscriber(subscriber) flatMap { _ =>
            throw SubscribeRegisterException("clean up complete", t)
        } recoverWith {
            case error =>
                val err = s"error while cleaning up subscriber $error"
                Logger.error(err)
                throw SubscribeRegisterException(err, t)
        }
    }
}