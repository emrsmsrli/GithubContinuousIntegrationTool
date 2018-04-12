package controllers

import controllers.cases.SubscriberRegister
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import play.api.Logger
import play.api.libs.json.{Json, Reads}
import repositories.models.GithubSubscriber
import services.SubscribeRegisterService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriberRegistrationController @Inject()(cc: ControllerComponents,
                                                 subscribeRegisterService: SubscribeRegisterService)
                                                (implicit ec: ExecutionContext) extends AbstractController(cc) {
    private implicit val srr: Reads[SubscriberRegister] = Json.reads[SubscriberRegister]

    def subscribe() : Action[SubscriberRegister] = Action.async(parse.json(srr)) { req =>
        Logger.debug("subscribe action captured")
        val subscriberRegister = req.body
        if(!subscriberRegister.isValid) {
            Logger.error(s"invalid input json $subscriberRegister")
            Future.successful(BadRequest("invalid register info"))
        }

        subscribeRegisterService.subscribe(GithubSubscriber(subscriberRegister.username, subscriberRegister.repository, subscriberRegister.token)).map { _ =>
            Logger.info("successfully subscribed")
            Ok(s"successfully subscribed to ${req.body.repository} for ${req.body.username}")
        } recover {
            case t: Throwable =>
                Logger.error(s"subscriberController finished with bad request: ${req.body}, error: $t")
                BadRequest("something went wrong while subscribing")
        }
    }
}
