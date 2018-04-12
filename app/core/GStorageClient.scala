package core

import com.google.cloud.storage._
import dispatchers.GCloudDispatcher
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.Future
import scala.collection.mutable.{Map => MutableMap}

case class GStorageException(msg: String, t: Throwable)
    extends Exception(msg, t)

@Singleton
class GStorageClient @Inject()(implicit gcd: GCloudDispatcher) {
    private val storage = StorageOptions.getDefaultInstance.getService
    private val blobInfos = MutableMap[String, BlobInfo]()

    def upload(fileName: String, bucketName: String, data: Array[Byte]): Future[String] = {
        val blobMapKey = s"$bucketName/$fileName"
        val blobInfo: BlobInfo = blobInfos.get(blobMapKey) match {
            case Some(info) => info
            case None =>
                val info = BlobInfo
                    .newBuilder(BlobId.of(bucketName, fileName))
                    .setContentType("application/zip").build()
                blobInfos.put(blobMapKey, info)
                info
        }
        Future {
            storage.create(blobInfo, data)
            s"https://storage.googleapis.com/$bucketName/$fileName"
        } recoverWith {
            case t: Throwable =>
                Logger.error(s"gcloud storage upload error filename: $fileName, bucket: $bucketName, $t")
                throw GStorageException("error while uploading to gcloud storage", t)
        }
    }
}
