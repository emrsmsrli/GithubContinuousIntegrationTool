package core

import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.ServiceOptions
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{ProjectTopicName, PubsubMessage}
import dispatchers.GCloudDispatcher
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future
import scala.collection.mutable.{Map => MutableMap}

case class PubSubException(msg: String, t: Throwable)
    extends Exception(msg, t)

@Singleton
class PubSubClient @Inject()(appLifecycle: ApplicationLifecycle)
                            (implicit gcd: GCloudDispatcher) {
    private val projectId = ServiceOptions.getDefaultProjectId
    private val publishers = MutableMap[String, Publisher]()

    appLifecycle.addStopHook { () =>
        Future.successful(
            for(publisher <- publishers.values)
                publisher.shutdown()
        )
    }

    def publish(topicName: String, message: String): Future[String] = {
        val publisher: Publisher = publishers.get(topicName) match {
            case Some(p) => p
            case None => val p = Publisher
                .newBuilder(ProjectTopicName.of(projectId, topicName))
                .build()
                publishers.put(topicName, p)
                p
        }

        Future {
            publisher.publish(PubsubMessage
                .newBuilder()
                .setData(ByteString.copyFromUtf8(message))
                .build())
                .get()
        } recoverWith {
            case t: Throwable =>
                Logger.error(s"error while publishing to pubsub, topic: $topicName, t: $t")
                throw PubSubException("publish error", t)
        }
    }
}
