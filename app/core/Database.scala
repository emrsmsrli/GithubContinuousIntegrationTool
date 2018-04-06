package core

import java.sql.Connection

import dispatchers.DatabaseDispatcher
import javax.inject.{Inject, Singleton}
import play.api.db.DBApi

import scala.concurrent.Future

@Singleton
class Database @Inject()(dbApi: DBApi)(implicit dd: DatabaseDispatcher) {
    private lazy val db = dbApi.database("default")

    def withConnection[A](block: Connection => A): Future[A] = {
        Future { db.withConnection { block(_) } }
    }
}