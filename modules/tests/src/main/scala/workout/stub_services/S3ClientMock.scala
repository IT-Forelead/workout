package workout.stub_services

import com.amazonaws.services.s3.model.Bucket
import com.itforelead.workout.domain.custom.refinements.FilePath
import com.itforelead.workout.services.s3.S3Client
import fs2.Pipe
import java.net.URL

class S3ClientMock[F[_]] extends S3Client[F] {
  override def listFiles: fs2.Stream[F, FilePath] = ???

  override def listBuckets: fs2.Stream[F, Bucket] = ???

  override def downloadObject(key: FilePath): fs2.Stream[F, Byte] = ???

  override def deleteObject(key: FilePath): fs2.Stream[F, Unit] = ???

  override def putObject(key: FilePath): Pipe[F, Byte, Unit] = ???

  override def uploadFileMultipart(key: FilePath, chunkSize: Int): Pipe[F, Byte, String] = ???

  override def objectUrl(key: FilePath): F[URL] = ???
}
