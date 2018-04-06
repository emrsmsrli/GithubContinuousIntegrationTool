package repositories

import core.Database
import javax.inject.{Inject, Singleton}
import play.api.Logger
import anorm._
import repositories.models.GithubSubscriber

import scala.concurrent.Future

@Singleton
class SubscriberRepository @Inject()(database: Database) {
    def insertSubscriber(subscriber: GithubSubscriber): Future[Option[Long]] = {
        database.withConnection { implicit c =>
            Logger.debug(s"inserting subscriber $subscriber")
            SQL"insert into subscriber(username, repository, token) values({un}, {repo}, {token})"
                .on('un -> subscriber.username, 'repo -> subscriber.repository, 'token -> subscriber.token)
                .executeInsert()
        }
    }

    def updateSubscriber(subscriber: GithubSubscriber): Future[Boolean] = {
        database.withConnection { implicit c =>
            Logger.debug(s"updating subscriber $subscriber")
            SQL"update subscriber set `webhook_url`={wh_url} where `username`={un} and `repository`={repo}"
                .on('wh_url -> subscriber.webhookUrl, 'un -> subscriber.username, 'repo -> subscriber.repository)
                .execute()
        }
    }

    def deleteSubscriber(subscriber: GithubSubscriber): Future[Boolean] = {
        database.withConnection { implicit c =>
            Logger.debug(s"deleting subscriber $subscriber")
            SQL"delete subscriber where `username`={un} and `repository`={repo}"
                .on('un -> subscriber.username, 'repo -> subscriber.repository)
                .execute()
        }
    }

    def getSubscriber(username: String, repo: String): Future[Option[GithubSubscriber]] = {
        database.withConnection { implicit c =>
            Logger.debug(s"trying to retrieve subscriber $username and $repo")
            val result: SqlQueryResult = SQL"select * from subscriber where `username`={un} and `repository`={repo}"
                .on('un -> username, 'repo -> repo)
                .executeQuery()
            result.resultSet.acquireFor { resultSet =>
                GithubSubscriber(
                    resultSet.getString("username"),
                    resultSet.getString("repository"),
                    resultSet.getString("token"),
                    resultSet.getString("webhook_url"),
                    resultSet.getInt("id")
                )
            }.either match {
                case Left(errors) =>
                    Logger.debug(s"could not find subscriber, ${errors.addString(new StringBuilder(),
                        "errors: [", ", ", "]")}")
                    None
                case Right(subscriber) =>
                    Logger.debug("found subscriber")
                    Some(subscriber)
            }
        }
    }
}