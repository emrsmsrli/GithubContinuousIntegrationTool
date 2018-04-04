package core

import java.sql.ResultSet

import scala.util.{Failure, Success, Try}

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.db.DBApi

import scala.concurrent.{ExecutionContext, Future, Promise}

case class SqlClientException(msg: String, cause: Throwable)
    extends RuntimeException(msg, cause)

@Singleton
class SqlClient @Inject()(dbApi: DBApi)(implicit ec: ExecutionContext) {
    private lazy val db = dbApi.database("default")

    def execute(sql: String, vals: List[String], f: ResultSet => Unit = null): Future[Int] =
        execute(SqlClient.escape(sql, vals), f)

    def execute(sql: String, f: ResultSet => Unit): Future[Int] = {
        val p = Promise[Int]()
        Future(db.withConnection { implicit conn =>
            val statement = conn.createStatement()
            statement.execute(sql)
            val results = statement.getResultSet
            if(results != null)
                f(results)
            p.success(statement.getUpdateCount)
        }) recover { case e =>
            val msg = s"could not execute sql: $sql"
            Logger.error(s"$msg: $e")
            p.failure(SqlClientException(msg, e))
        }
        p.future
    }
}

object SqlClient {
    def escape(sql: String, vals: List[String]): String = {
        if(vals.isEmpty || !sql.contains("?"))
            return sql

        val head :: tail = vals

        Try { head.toInt } match {
            case Success(_) =>
                escape(sql.replaceFirst("\\?", head), tail)
            case Failure(_) =>
                escape(sql.replaceFirst("\\?", s"'$head'"), tail)
        }
    }
}