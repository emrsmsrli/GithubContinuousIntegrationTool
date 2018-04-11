
package object services {
    def formatGithubHookUrl(username: String, repository: String): String = {
        s"https://api.github.com/repos/$username/$repository/hooks"
    }

    def formatPubSubUrl(subscriberId: Long)
        = s"https://f9146eee.ngrok.io/push/$subscriberId"
}
