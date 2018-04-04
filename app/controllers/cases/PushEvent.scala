package controllers.cases

case class PushEventPusher(name: String)
case class PushEventCommit(id: String)
case class PushEvent(commits: Seq[PushEventCommit], pusher: PushEventPusher)