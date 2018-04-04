package controllers

import controllers.cases.SubscriberRegister
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import utils.Implicits._
import play.api.Logger
import services.SubscribeService

import scala.concurrent.ExecutionContext

@Singleton
class SubscriberController @Inject()(cc: ControllerComponents,
                                     subscribeService: SubscribeService)
                                    (implicit ec: ExecutionContext)
    extends AbstractController(cc) {

    def subscribe(): Action[SubscriberRegister] = Action.async(parse.json(gsrr)) { req =>
        Logger.info("subscribe action captured")
        subscribeService.subscribe(req.body).map { _ =>
            Logger.info("successfully subscribed")
            Ok(s"successfully subscribed to ${req.body.repository} for ${req.body.username}")
        } recover {
            case e =>
                Logger.error(s"subscriberController finished with badRequest: " +
                    s"${req.body}, e: $e")
                BadRequest("something went wrong while subscribing")
        }
    }
}
