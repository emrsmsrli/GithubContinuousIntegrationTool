package controllers

import controllers.cases.SubscriberRegister
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import utils.Implicits.implicitSr
import play.api.Logger
import repositories.models.GithubSubscriber
import services.SubscribeRegisterService

import scala.concurrent.ExecutionContext

@Singleton
class SubscriberRegistrationController @Inject()(cc: ControllerComponents,
                                                 subscribeRegisterService: SubscribeRegisterService)
                                                (implicit ec: ExecutionContext) extends AbstractController(cc) {
    def subscribe() : Action[SubscriberRegister] = Action.async(parse.json(implicitSr)) { req =>
        Logger.info("subscribe action captured")
        val subscriberRegister = req.body
        subscribeRegisterService.subscribe(GithubSubscriber(subscriberRegister.username, subscriberRegister.repository, subscriberRegister.token)).map { _ =>
            Logger.info("successfully subscribed")
            Ok(s"successfully subscribed to ${req.body.repository} for ${req.body.username}")
        } recover {
            case e: Throwable =>
                Logger.error(s"subscriberController finished with bad request: ${req.body}, error: $e")
                BadRequest("something went wrong while subscribing")
        }
    }
}
