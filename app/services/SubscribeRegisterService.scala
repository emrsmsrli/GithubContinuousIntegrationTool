package services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import repositories.SubscriberRepository
import repositories.models.GithubSubscriber

import scala.concurrent.{ExecutionContext, Future}

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
            case error: Throwable =>
                Logger.error(s"error occurred while subscribing $error")
                deleteSubscriberIfError(subscriber)
        }
    }

    private def checkSubscriberAlreadyExists(subscriber: GithubSubscriber): Future[Boolean] = {
        Logger.debug(s"checking if subscriber already exists: $subscriber")
        subscriberRepository.getSubscriber(subscriber.username, subscriber.repository) map {
            case Some(_) =>
                Logger.debug("subscriber found")
                true
            case None =>
                Logger.debug("subscriber not found")
                false
        }
    }

    private def createSubscriberIfNotExists(subscriber: GithubSubscriber, doesExist: Boolean): Future[Option[Long]] = {
        if(!doesExist) {
            subscriberRepository.insertSubscriber(subscriber)
        } else {
            val err = "subscriber already exists"
            Logger.error(err)
            Future.failed(new RuntimeException(err))
        }
    }

    private def registerWebhookIfCreated(subscriber: GithubSubscriber, maybeInsertId: Option[Long])
            : Future[Option[GithubHookResponse]] = {
        maybeInsertId match {
            case Some(insertId) =>
                githubRequestsService.registerWebhook(subscriber.copy(id = insertId))
            case None =>
                val err = "could not insert subscriber"
                Logger.error(err)
                Future.failed(new RuntimeException(err))
        }
    }

    private def updateSubscriberIfRegistered(subscriber: GithubSubscriber, maybeResponse: Option[GithubHookResponse])
            : Future[Boolean] = {
        maybeResponse match {
            case Some(subscribed) =>
                subscriberRepository.updateSubscriber(subscriber.copy(webhookUrl = subscribed.url))
            case None =>
                Future.failed(new RuntimeException("could not register webhook"))
        }
    }

    private def deleteSubscriberIfError(subscriber: GithubSubscriber): Future[Boolean] = {
        subscriberRepository.deleteSubscriber(subscriber) recoverWith {
            case error =>
                val err = s"error while cleaning up subscriber $error"
                Logger.error(err)
                Future.failed(new RuntimeException(err))
        }
    }
}