package core

import java.sql.Connection

import dispatchers.DatabaseDispatcher
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.db.DBApi

import scala.concurrent.Future

case class DatabaseException(msg: String, t: Throwable) extends Exception(msg, t)

@Singleton
class Database @Inject()(dbApi: DBApi)(implicit dd: DatabaseDispatcher) {
    private lazy val db = dbApi.database("default")

    def withConnection[A](block: Connection => A): Future[A] = {
        Future { db.withConnection { block(_) } } recover {
            case t =>
                Logger.error(s"database error $t")
                throw DatabaseException("database error occurred", t)
        }
    }
}