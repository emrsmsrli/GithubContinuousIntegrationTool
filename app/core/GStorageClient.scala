package core

import com.google.cloud.storage._
import dispatchers.GCloudDispatcher
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.Future

case class GStorageException(msg: String, cause: Throwable)
    extends RuntimeException(msg, cause)

@Singleton
class GStorageClient @Inject()(implicit gcd: GCloudDispatcher) {
    private val storage = StorageOptions.getDefaultInstance.getService
    private val blobInfos = scala.collection.mutable.Map[String, BlobInfo]()

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
            // TODO fix return link `https://storage.googleapis.com/${this.bucketName}/${path.basename(p)}`;
            Logger.info(blobInfo.getSelfLink + bucketName + fileName)
            blobInfo.getSelfLink + bucketName + fileName
        }.recoverWith {
            case t: Throwable =>
                Logger.error(s"gcloud storage upload error filename: $fileName, bucket: $bucketName, $t")
                Future.failed(GStorageException("error while uploading to gcloud storage", t))
        }
    }
}
