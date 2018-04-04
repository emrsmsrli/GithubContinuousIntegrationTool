package repositories

import core.SqlClient
import javax.inject.{Inject, Singleton}
import play.api.Logger
import repositories.models.GithubSubscriber

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

@Singleton
class SubscriberRepository @Inject()(sqlClient: SqlClient)
                                    (implicit ec: ExecutionContext) {

    def insertSubscriber(githubSubscriber: GithubSubscriber): Future[Boolean] = {
        val promise = Promise[Boolean]()
        sqlClient.execute("insert into subscriber set `username`=?, `repository`=?, `token`=?;",
            List(githubSubscriber.username, githubSubscriber.repository, githubSubscriber.token)) onComplete {
            case Success(updateCount) =>
                promise.success(updateCount != 0)
            case Failure(err) =>
                Logger.error(s"error while inserting subscriber: $githubSubscriber")
                promise.failure(err)
        }
        promise.future
    }

    def updateSubscriber(githubSubscriber: GithubSubscriber): Future[Boolean] = {
        val promise = Promise[Boolean]()
        sqlClient.execute("update subscriber set `webhook_url`=? where `username`=? and `repository`=?;",
            List(githubSubscriber.webhookUrl, githubSubscriber.username, githubSubscriber.repository)) onComplete {
            case Success(updateCount) =>
                promise.success(updateCount != 0)
            case Failure(err) =>
                Logger.error(s"error while updating subscriber: $githubSubscriber")
                promise.failure(err)
        }
        promise.future
    }

    def deleteSubscriber(githubSubscriber: GithubSubscriber): Future[Boolean] = {
        val promise = Promise[Boolean]()
        sqlClient.execute("delete subscriber where `username`=? and `repository`=?;",
            List(githubSubscriber.username, githubSubscriber.repository)) onComplete {
            case Success(updateCount) =>
                promise.success(updateCount != 0)
            case Failure(err) =>
                Logger.error(s"error while deleting subscriber: $githubSubscriber")
                promise.failure(err)
        }
        promise.future
    }

    def getSubscriber(username: String, repo: String): Future[GithubSubscriber] = {
        val promise = Promise[GithubSubscriber]()
        sqlClient.execute("select * from subscriber where `username`=? and `repository`=?;",
            List(username, repo), { subscribers =>
                if(!subscribers.next()) {
                    promise.success(null)
                } else {
                    promise.success(GithubSubscriber(
                        subscribers.getString("username"),
                        subscribers.getString("repository"),
                        subscribers.getString("token"),
                        subscribers.getString("webhook_url"),
                        subscribers.getInt("id")))
                }
            }) recover {
            case err =>
                Logger.error(s"error while retrieving subscriber username: $username, repo: $repo")
                promise.failure(err)
        }
        promise.future
    }

}