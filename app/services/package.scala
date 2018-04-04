
package object services {
    def formatGithubHookUrl(username: String, repository: String): String = {
        s"https://api.github.com/repos/$username/$repository/hooks"
    }

    // TODO fix this
    def formatPubSubUrl(subscriberId: Int)
        = s"https://us-central1-linovi-188707.cloudfunctions.net/github-webhook/push/$subscriberId"
}
