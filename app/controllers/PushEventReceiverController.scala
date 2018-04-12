package controllers

import controllers.cases.{PushEvent, PushEventCommit, PushEventPusher}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc._
import services.{PushEventService, PushRequest}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PushEventReceiverController @Inject()(cc: ControllerComponents,
                                            pushEventService: PushEventService)
                                           (implicit ec: ExecutionContext) extends AbstractController(cc) {
    private implicit val pecr: Reads[PushEventCommit] = Json.reads[PushEventCommit]
    private implicit val pepr: Reads[PushEventPusher] = Json.reads[PushEventPusher]
    private implicit val per: Reads[PushEvent] = Json.reads[PushEvent]

    def processPush(id: Long): Action[AnyContent] = Action.async { req =>
        Logger.debug(s"subscriber id is $id")

        req.headers.get("X-Github-Event") match {
            case Some(eventType) => eventType match {
                case event if event == "ping" =>
                    Logger.debug("ping event received")
                    Future.successful(Ok("ping event received"))
                case event if event == "push" =>
                    Logger.debug("push event received")
                    parsePushEvent(req.body.asJson) match {
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

    private def parsePushEvent(maybeJson: Option[JsValue]): Option[PushEvent] = {
        maybeJson match {
            case Some(json) => Json.fromJson[PushEvent](json).asOpt
            case None => None
        }
    }

    private def processPushEvent(id: Long, pushEvent: PushEvent): Future[Result] = {
        pushEventService.processPush(PushRequest(id, pushEvent)).map { _ =>
            Logger.info("successfully handled push event")
            Ok("successfully handled push event")
        } recover { case t: Throwable =>
            Logger.error(s"push controller finished with error: $t")
            BadRequest("process push failed")
        }
    }
}
