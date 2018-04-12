package controllers

import controllers.cases.{PubSubEvent, PubSubEventMessage}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import java.util.Base64

import play.api.libs.json.{Json, Reads}
import services.PubSubEventHandlerService
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PubSubReceiverController @Inject()(cc: ControllerComponents,
                                         eventHandlerService: PubSubEventHandlerService)
                                        (implicit ec: ExecutionContext) extends AbstractController(cc) {
    private implicit val psemr: Reads[PubSubEventMessage] = Json.reads[PubSubEventMessage]
    private implicit val pser: Reads[PubSubEvent] = Json.reads[PubSubEvent]

    def receiveMessage(): Action[PubSubEvent] = Action.async(parse.json(pser)) { req =>
        val event = req.body
        Logger.debug(s"incoming event: $event")
        val decodedEvent = new String(Base64.getDecoder.decode(event.message.data))
        Logger.debug(decodedEvent)
        eventHandlerService.processEvent(decodedEvent) map { _ => Ok("") } recoverWith {
            case t: Throwable =>
                Logger.error(s"error on pubsub receiver $t")
                Future.successful(BadRequest("pubSubReceiver ended with failure"))
        }
    }

    def verificationHtml() = Action { Ok(views.html.google3ac3553bfe0d3b2b()) }
}
