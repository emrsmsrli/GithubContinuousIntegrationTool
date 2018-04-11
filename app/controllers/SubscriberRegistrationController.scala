package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import utils.Implicits.implicitGs
import play.api.Logger
import repositories.models.GithubSubscriber
import services.SubscribeRegisterService

import scala.concurrent.ExecutionContext

@Singleton
class SubscriberRegistrationController @Inject()(cc: ControllerComponents,
                                                 subscribeRegisterService: SubscribeRegisterService)
                                                (implicit ec: ExecutionContext) extends AbstractController(cc) {
    def subscribe() : Action[GithubSubscriber] = Action.async(parse.json(implicitGs)) { req =>
        Logger.info("subscribe action captured")
        subscribeRegisterService.subscribe(req.body).map { _ =>
            Logger.info("successfully subscribed")
            Ok(s"successfully subscribed to ${req.body.repository} for ${req.body.username}")
        } recover {
            case e: Throwable =>
                Logger.error(s"subscriberController finished with bad request: ${req.body}, error: $e")
                BadRequest("something went wrong while subscribing")
        }
    }
}
