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
            SQL"""
                  insert into push(pusher, commit_count, status, subscriber_id)
                  values({pusher}, {cCount}, {status}, {subsId})
            """.on('pusher -> githubPush.pusher, 'cCount -> githubPush.commitCount,
                    'status -> githubPush.status, 'subsId -> githubPush.subscriberId)
                .executeInsert()
        }
    }

    def updatePush(githubPush: GithubPush): Future[Boolean] = {
        database.withConnection { implicit c =>
            Logger.debug(s"updating push $githubPush")
            SQL"""
                  update push set `zip_url`={zipUrl}, `status`={status}, `subscriber_id`={subsId}
                  where `id`={id} and `status`={statusBeforeUpdate}
            """.on('zipUrl -> githubPush.zipUrl, 'status -> githubPush.status,
                    'subsId -> githubPush.subscriberId, 'id -> githubPush.id, 'statusBeforeUpdate -> "INPROGRESS")
                .execute()
        }
    }
}
