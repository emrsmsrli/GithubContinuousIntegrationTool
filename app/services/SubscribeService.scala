package services

import controllers.cases.SubscriberRegister
import core.RequestBuilder
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import repositories.SubscriberRepository
import repositories.models.GithubSubscriber
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

case class GithubHookResponse(id: Int, url: String)
case class GithubHookResponseError(message: String)
case class GithubHookBodyConfig(url: String, content_type: String)
case class GithubHookBody(name: String, config: GithubHookBodyConfig)

@Singleton
class SubscribeService @Inject()(subscriberRepository: SubscriberRepository,
                                 requestBuilder: RequestBuilder)
                                (implicit ec: ExecutionContext) {

    def subscribe(githubSubscriberRegister: SubscriberRegister): Future[Boolean] = {
        val subscriber = GithubSubscriber(githubSubscriberRegister.username,
                githubSubscriberRegister.repository,
                githubSubscriberRegister.token)

        Logger.info("trying to get existing subscriber")
        subscriberRepository.getSubscriber(githubSubscriberRegister.username, githubSubscriberRegister.repository)
            .flatMap { gSubscriber =>
                if (gSubscriber != null)
                    Future.failed(new RuntimeException(s"subscriber already exists $gSubscriber"))
                else {
                    Logger.info("subscriber not found, inserting")
                    subscriberRepository.insertSubscriber(subscriber)
                }
            }
            .flatMap { inserted =>
                if(inserted)
                    subscriberRepository.getSubscriber(githubSubscriberRegister.username,
                        githubSubscriberRegister.repository)
                else
                    Future.failed(new RuntimeException(s"subscriber not inserted"))}
            .flatMap { subscriber =>
                Logger.info("insert successful, sending request to github")
                requestBuilder
                    .url(formatGithubHookUrl(githubSubscriberRegister.username,
                        githubSubscriberRegister.repository))
                    .headers("User-Agent" -> "Akka",
                        "Authorization" -> s"token ${githubSubscriberRegister.token}")
                    .body(newBody(subscriber.id))
                    .send()
            }
            .flatMap { body =>
                Logger.info("github response arrived, trying to parse")

                val parsedJs = Json.parse(body)
                Logger.info(s"body dump $parsedJs")
                Json.fromJson(parsedJs)(grr).asOpt match {
                    case None =>
                        Json.fromJson(parsedJs)(ghre).asOpt match {
                            case None =>
                                Future.failed(new RuntimeException(s"github reponse failed to parse"))
                            case err =>
                                Future.failed(new RuntimeException(s"github response error ${err.get.message}"))
                        }
                    case res =>
                        Logger.info("parse successful, trying to update subscriber")
                        subscriberRepository.updateSubscriber(subscriber.copy(webhookUrl = res.get.url))
                }
            }
            .recoverWith {
                case t =>
                    Logger.error(s"error while subscribing $t, trying to clean up")
                    subscriberRepository.deleteSubscriber(subscriber) recoverWith {
                        case err =>
                            Logger.error(s"error while cleaning up subscriber $subscriber, $err")
                            Future.failed(new RuntimeException("error while cleaning up subscriber", err))
                    }
            }
    }

    private def newBody(subscriberId: Int): String = {
        Logger.info(s"creating new github request body for $subscriberId")
        val js = Json.toJson(GithubHookBody("web", GithubHookBodyConfig(formatPubSubUrl(subscriberId), "json")))
        Json.stringify(js)
    }
}