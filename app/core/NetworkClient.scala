package core

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

case class NetworkException(msg: String, cause: Throwable)
    extends RuntimeException(msg, cause)

@Singleton
class RequestBuilder @Inject()(implicit as: ActorSystem, ec: ExecutionContext) {
    private var uri: String = ""
    private var body: String = ""
    private var method = HttpMethods.GET
    private val hdrs = scala.collection.mutable.Map[String, String]()

    def url(url: String): RequestBuilder = {
        this.uri = url
        this
    }

    def body(body: String): RequestBuilder = {
        this.body = body
        this.method = HttpMethods.POST
        this
    }

    def headers(hdrs: (String, String)*): RequestBuilder = {
        this.hdrs ++= hdrs
        this
    }

    def send(): Future[String] = {
        val hdrsParsed = for(hdr <- hdrs) yield HttpHeader.parse(hdr._1, hdr._2) match {
            case HttpHeader.ParsingResult.Ok(h, _) => h
            case _ => null
        }

        val entity = if(body.isEmpty) HttpEntity.Empty else HttpEntity(ContentTypes.`application/json`, body)

        val promise = Promise[String]()
        Http().singleRequest(HttpRequest(method, uri, hdrsParsed.filter(_ != null).toList, entity))
            .flatMap { res =>
                res.entity.dataBytes.runFold(ByteString(""))(_ ++ _)(ActorMaterializer()).map(_.utf8String)
            } onComplete {
                case Success(bdy) =>
                    promise.success(bdy)
                    clear()
                case Failure(e) =>
                    val err = "error on http request"
                    Logger.error(err)
                    clear()
                    promise.failure(NetworkException(err, e))
            }
        promise.future
    }

    private def clear(): Unit = {
        method = HttpMethods.GET
        uri = ""
        body = ""
        hdrs.clear()
    }
}