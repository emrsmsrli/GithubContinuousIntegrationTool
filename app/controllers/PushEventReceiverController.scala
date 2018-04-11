package controllers

import controllers.cases.PushEvent
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import services.{PushRequest, PushEventService}
import utils.Implicits.gpr

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PushEventReceiverController @Inject()(cc: ControllerComponents,
                                            pushEventService: PushEventService)(implicit ex: ExecutionContext)
    extends AbstractController(cc) {

    def processPush(id: Long): Action[AnyContent] = Action.async { req =>
        Logger.info(s"subscriber id is $id")

        req.headers.get("X-Github-Event") match {
            case Some(eventType) => eventType match {
                case event if event == "ping" =>
                    Logger.info("ping event received")
                    Future.successful(Ok("ping event received"))
                case event if event == "push" =>
                    Logger.info("push event received")
                    parsePushEvent(req.body) match {
                        case Some(parsedEvent) => processPushEvent(id, parsedEvent)
                        case None => Future.successful(BadRequest("could not parse github event body"))
                    }
            }
            case None =>
                Logger.error("no x-github-event header found")
                Future.successful(BadRequest("no x-github-event header"))
        }
    }

    def missingId() = Action { BadRequest("missing id") }

    private def parsePushEvent(reqBody: AnyContent): Option[PushEvent] = {
        reqBody.asJson match {
            case Some(json) => Json.fromJson[PushEvent](json).asOpt
            case None => None
        }
    }

    private def processPushEvent(id: Long, pushEvent: PushEvent): Future[Result] = {
        pushEventService.processPush(PushRequest(id, pushEvent)).map { _ =>
            Logger.info("successfully handled push event")
            Ok("successfully handled push event")
        } recover { case err: Throwable =>
            Logger.error(s"push controller finished with error: $err")
            BadRequest("process push failed")
        }
    }
}
