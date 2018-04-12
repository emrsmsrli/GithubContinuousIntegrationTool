package utils

import play.api.libs.json.{Json, Reads}
import services.{GithubHookResponse, SubscriberPushIds}

object Implicits {
    implicit val implicitGhr: Reads[GithubHookResponse] = Json.reads[GithubHookResponse]
    implicit val implicitSpids: Reads[SubscriberPushIds] = Json.reads[SubscriberPushIds]
}
