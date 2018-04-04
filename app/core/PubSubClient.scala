package core

import com.google.api.core.{ApiFutureCallback, ApiFutures}
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.ServiceOptions
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{PubsubMessage, TopicName}
import javax.inject.Singleton
import play.api.Logger

import scala.concurrent.{Future, Promise}

case class PubSubException(msg: String, cause: Throwable)
    extends RuntimeException(msg, cause)

@Singleton
class PubSubClient {
    private val projectId = ServiceOptions.getDefaultProjectId

    def publish(topicName: String, message: String): Future[String] = {
        val promise = Promise[String]()
        val publisher = Publisher
            .newBuilder(TopicName.of(projectId, topicName))
            .build()
        val data = ByteString.copyFromUtf8(message)
        val apiFuture = publisher.publish(PubsubMessage
            .newBuilder()
            .setData(data)
            .build())
        ApiFutures.addCallback(apiFuture, callback(publisher, promise, topicName))
        promise.future
    }

    private def callback(publisher: Publisher, promise: Promise[String], topicName: String) = {
        new ApiFutureCallback[String]() {
            override def onFailure(t: Throwable): Unit = {
                val err = s"error while publishing to pubsub, topic: $topicName"
                Logger.error(err)
                publisher.shutdown()
                promise.failure(PubSubException(err, t))
            }

            override def onSuccess(result: String): Unit = {
                publisher.shutdown()
                promise.success(result)
            }
        }
    }
}
