package repositories.models

trait GithubPushStatus {
    def string(): String
}
object InProgress extends GithubPushStatus {
    override def string() = "INPROGRESS"
}
object Done extends GithubPushStatus {
    override def string() = "DONE"
}

case class GithubPush(pusher: String,
                      commitCount: Int,
                      subscriberId: Int,
                      status: GithubPushStatus = InProgress,
                      id: Int = 0,
                      zip_url: String = "")