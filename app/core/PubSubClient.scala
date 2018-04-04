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
        try {
            val apiFuture = publisher.publish(PubsubMessage
                .newBuilder()
                .setData(ByteString.copyFromUtf8(message))
                .build())

            ApiFutures.addCallback(apiFuture, new ApiFutureCallback[String] {
                override def onFailure(t: Throwable): Unit = {
                    Logger.error(s"publish error $t")
                    promise.failure(PubSubException(s"error while publishing to pubsub, topic: $topicName", t))
                }
                override def onSuccess(result: String): Unit = {
                    promise.success(result)
                }
            })
        } finally {
            publisher.shutdown()
        }

        promise.future
    }
}
