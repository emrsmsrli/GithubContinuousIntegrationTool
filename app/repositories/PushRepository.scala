package repositories

import core.Database
import javax.inject.{Inject, Singleton}
import play.api.Logger
import repositories.models.GithubPush
import anorm._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PushRepository @Inject()(database: Database)
                              (implicit ec: ExecutionContext) {
    def insertPush(githubPush: GithubPush): Future[Option[Long]] = {
        database.withConnection { implicit c =>
            Logger.debug(s"inserting push $githubPush")
            SQL"insert into push(pusher, commit_count, status, subscriber_id) values(${
                githubPush.pusher}, ${githubPush.commitCount}, ${githubPush.status}, ${githubPush.subscriberId})"
                .executeInsert()
        }
    }

    def updatePush(githubPush: GithubPush): Future[Boolean] = {
        database.withConnection { implicit c =>
            Logger.debug(s"updating push $githubPush")
            SQL"update push set `zip_url`=${githubPush.zipUrl}, `status`=${githubPush.status} where `subscriber_id`=${
                githubPush.subscriberId} and `id`=${githubPush.id} and `status`='INPROGRESS'"
                .execute()
        }
    }
}
