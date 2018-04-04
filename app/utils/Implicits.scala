package utils

import controllers.cases.{PushEvent, PushEventCommit, PushEventPusher, SubscriberRegister}
import play.api.libs.json.{Json, Reads, Writes}
import services.{GithubHookBody, GithubHookBodyConfig, GithubHookResponse, GithubHookResponseError}

object Implicits {
    implicit val gpcr: Reads[PushEventCommit] = Json.reads[PushEventCommit]
    implicit val gppr: Reads[PushEventPusher] = Json.reads[PushEventPusher]
    implicit val gpr: Reads[PushEvent] = Json.reads[PushEvent]
    implicit val gsrr: Reads[SubscriberRegister] = Json.reads[SubscriberRegister]

    implicit val ghbcw: Writes[GithubHookBodyConfig] = Json.writes[GithubHookBodyConfig]
    implicit val ghbw: Writes[GithubHookBody] = Json.writes[GithubHookBody]
    implicit val grr: Reads[GithubHookResponse] = Json.reads[GithubHookResponse]
    implicit val ghre: Reads[GithubHookResponseError] = Json.reads[GithubHookResponseError]
}
