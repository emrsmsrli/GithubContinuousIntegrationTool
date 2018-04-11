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
            SQL"insert into subscriber(username, repository, token) values(${
                subscriber.username}, ${subscriber.repository}, ${subscriber.token})"
                .executeInsert()
        }
    }

    def updateSubscriber(subscriber: GithubSubscriber): Future[Boolean] = {
        database.withConnection { implicit c =>
            Logger.debug(s"updating subscriber $subscriber")
            SQL"update subscriber set `webhook_url`=${subscriber.webhookUrl} where `username`=${
                subscriber.username} and `repository`=${subscriber.repository}"
                .execute()
        }
    }

    def deleteSubscriber(subscriber: GithubSubscriber): Future[Boolean] = {
        database.withConnection { implicit c =>
            Logger.debug(s"deleting subscriber $subscriber")
            SQL"delete from subscriber where `username`=${subscriber.username} and `repository`=${subscriber.repository}"
                .execute()
        }
    }

    def getSubscriber(id: Long): Future[Option[GithubSubscriber]] = {
        Logger.debug(s"trying to retrieve subscriber $id")
        getSubscriber(SQL"select * from subscriber where `id`=$id")
    }

    def getSubscriber(username: String, repo: String): Future[Option[GithubSubscriber]] = {
        Logger.debug(s"trying to retrieve subscriber $username and $repo")
        getSubscriber(SQL"select * from subscriber where `username`=$username and `repository`=$repo")
    }

    private def getSubscriber(sql: SimpleSql[Row]) = {
        database.withConnection { implicit c =>
            val result: SqlQueryResult = sql.executeQuery()
            result.resultSet.acquireFor { resultSet =>
                resultSet.first()
                GithubSubscriber(
                    resultSet.getString("username"),
                    resultSet.getString("repository"),
                    resultSet.getString("token"),
                    resultSet.getString("webhook_url"),
                    resultSet.getInt("id")
                )
            }.either match {
                case Left(errors) =>
                    Logger.debug(s"could not find subscriber, ${errors.mkString(",")}")
                    None
                case Right(subscriber) =>
                    Logger.debug("found subscriber")
                    Some(subscriber)
            }
        }
    }
}