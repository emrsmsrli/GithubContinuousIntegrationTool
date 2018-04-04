package repositories

import core.SqlClient
import javax.inject.{Inject, Singleton}
import play.api.Logger
import repositories.models.{Done, GithubPush, InProgress}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

@Singleton
class PushRepository @Inject()(sqlClient: SqlClient)
                              (implicit ec: ExecutionContext) {
    def insertPush(githubPush: GithubPush): Future[Int] = {
        val promise = Promise[Int]()
        sqlClient.execute("insert into push set `pusher`=?, `commit_count`=?, `status`=?, `subscriber_id`=?;",
            List(githubPush.pusher, githubPush.commitCount.toString,
                InProgress.string(), githubPush.subscriberId.toString), { res =>
            res.next()
            promise.success(res.getInt("id"))
        }) recover {
            case err =>
                Logger.error(s"error while inserting push data: $githubPush")
                promise.failure(err)
        }
        promise.future
    }

    def updatePush(githubPush: GithubPush): Future[Boolean] = {
        val promise = Promise[Boolean]()
        sqlClient.execute("update push set `zip_url`=?, `status`=?, `subscriber_id`=? where `id`=? and `status`=?;",
            List(githubPush.zip_url, Done.string(), githubPush.subscriberId.toString,
                githubPush.id.toString, InProgress.string())) onComplete {
            case Success(res) =>
                promise.success(res != 0)
            case Failure(err) =>
                Logger.error(s"error while updating push data: $githubPush")
                promise.failure(err)
        }
        promise.future
    }
}
