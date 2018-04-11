package utils

import controllers.cases._
import play.api.libs.json.{Json, Reads}
import repositories.models.GithubSubscriber
import services.{GithubErrorResponse, GithubHookResponse}

object Implicits {
    implicit val gpcr: Reads[PushEventCommit] = Json.reads[PushEventCommit]
    implicit val gppr: Reads[PushEventPusher] = Json.reads[PushEventPusher]
    implicit val gpr: Reads[PushEvent] = Json.reads[PushEvent]

    implicit val implicitSr: Reads[SubscriberRegister] = Json.reads[SubscriberRegister]

    implicit val implicitGer: Reads[GithubErrorResponse] = Json.reads[GithubErrorResponse]
    implicit val implicitGhr: Reads[GithubHookResponse] = Json.reads[GithubHookResponse]
    implicit val implicitGs: Reads[GithubSubscriber] = Json.reads[GithubSubscriber]

    implicit val implicitPsem: Reads[PubSubEventMessage] = Json.reads[PubSubEventMessage]
    implicit val implicitPse: Reads[PubSubEvent] = Json.reads[PubSubEvent]
}
