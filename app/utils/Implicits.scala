package utils

import controllers.cases._
import play.api.libs.json.{Json, Reads}
import services.{GithubErrorResponse, GithubHookResponse, SubscriberPushIds}

object Implicits {
    implicit val gpcr: Reads[PushEventCommit] = Json.reads[PushEventCommit]
    implicit val gppr: Reads[PushEventPusher] = Json.reads[PushEventPusher]
    implicit val gpr: Reads[PushEvent] = Json.reads[PushEvent]

    implicit val implicitSr: Reads[SubscriberRegister] = Json.reads[SubscriberRegister]

    implicit val implicitGer: Reads[GithubErrorResponse] = Json.reads[GithubErrorResponse]
    implicit val implicitGhr: Reads[GithubHookResponse] = Json.reads[GithubHookResponse]

    implicit val implicitPsem: Reads[PubSubEventMessage] = Json.reads[PubSubEventMessage]
    implicit val implicitPse: Reads[PubSubEvent] = Json.reads[PubSubEvent]

    implicit val implicitSpids: Reads[SubscriberPushIds] = Json.reads[SubscriberPushIds]
}
