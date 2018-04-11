
package object services {
    def formatGithubHookUrl(username: String, repository: String)
        = s"https://api.github.com/repos/$username/$repository/hooks"

    def formatGithubZipballUrl(username: String, repository: String)
        = s"https://api.github.com/repos/$username/$repository/zipball/master"

    def formatPubSubUrl(subscriberId: Long)
        = s"https://f9146eee.ngrok.io/push/$subscriberId"
}
