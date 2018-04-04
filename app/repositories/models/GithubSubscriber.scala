package repositories.models

case class GithubSubscriber(username: String,
                            repository: String,
                            token: String,
                            webhookUrl: String = null,
                            id: Int = 0)
