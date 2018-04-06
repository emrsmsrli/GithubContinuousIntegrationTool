
package object services {
    def formatGithubHookUrl(username: String, repository: String): String = {
        s"https://api.github.com/repos/$username/$repository/hooks"
    }

    def formatPubSubUrl(subscriberId: Long)
        = s"https://us-central1-linovi-188707.cloudfunctions.net/github-webhook/push/$subscriberId"
}
