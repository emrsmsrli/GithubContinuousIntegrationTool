package core

import com.google.api.core.{ApiFuture, ApiFutureCallback, ApiFutures}
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.ServiceOptions
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{ProjectTopicName, PubsubMessage}
import dispatchers.GCloudDispatcher
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{Future, Promise}

case class PubSubException(msg: String, cause: Throwable)
    extends RuntimeException(msg, cause)

@Singleton
class PubSubClient @Inject()(appLifecycle: ApplicationLifecycle)
                            (implicit gcd: GCloudDispatcher) {
    private val projectId = ServiceOptions.getDefaultProjectId
    private val publishers = scala.collection.mutable.Map[String, Publisher]()

    appLifecycle.addStopHook { () =>
        Future.successful(
            for(publisher <- publishers.values)
                publisher.shutdown()
        )
    }

    def publish(topicName: String, message: String): Future[String] = {
        val promise = Promise[String]()
        val publisher: Publisher = publishers.get(topicName) match {
            case Some(p) => p
            case None => val p = Publisher
                .newBuilder(ProjectTopicName.of(projectId, topicName))
                .build()
                publishers.put(topicName, p)
                p
        }

        val messageIdFuture: ApiFuture[String] = publisher.publish(PubsubMessage
            .newBuilder()
            .setData(ByteString.copyFromUtf8(message))
            .build())

        ApiFutures.addCallback(messageIdFuture, new ApiFutureCallback[String] {
            override def onFailure(t: Throwable): Unit = {
                Logger.error(s"publish error $t")
                promise.failure(PubSubException(s"error while publishing to pubsub, topic: $topicName", t))
            }
            override def onSuccess(result: String): Unit = {
                promise.success(result)
            }
        })

        promise.future
    }
}
