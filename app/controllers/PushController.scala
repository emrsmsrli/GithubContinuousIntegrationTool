package controllers

import controllers.cases.PushEvent
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.{PushRequest, PushService}
import utils.Implicits.gpr

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PushController @Inject()(cc: ControllerComponents,
                               pushService: PushService)(implicit ex: ExecutionContext)
    extends AbstractController(cc) {

    def processPush(id: Int): Action[AnyContent] = Action.async { req =>
        Logger.info(s"subscriber id is $id")

        req.headers.get("X-Github-Event") match {
            case eventType => eventType.get match {
                case event if event == "ping" =>
                    Logger.info("ping event received")
                    Future.successful(Ok("ping event received"))
                case event if event == "push" =>
                    Logger.info("push event received")
                    parsePushEvent(req.body) match {
                        case parsedEvent =>
                            pushService.processPush(PushRequest(id, parsedEvent.get)).map { _ =>
                                Logger.info("successfully handled push event")
                                Ok("successfully handled push event")
                            } recover { case _ =>
                                BadRequest("process push failed")
                            }
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
            case e => Json.fromJson(e.get).asOpt match {
                case parsed => Option(parsed.get)
                case None => Option.empty
            }
            case None => Option.empty
        }
    }
}
