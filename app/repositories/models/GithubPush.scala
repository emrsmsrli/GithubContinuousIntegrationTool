package repositories.models

case class GithubPush(pusher: String,
                      commitCount: Int,
                      subscriberId: Long,
                      status: String = "INPROGRESS",
                      id: Long = 0,
                      zipUrl: String = "")