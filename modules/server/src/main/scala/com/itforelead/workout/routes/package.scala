package com.itforelead.workout

import cats.effect.Sync
import cats.syntax.all._
import com.itforelead.workout.domain.custom.refinements.{FileKey, FileName, FilePath}
import com.itforelead.workout.effects.GenUUID
import eu.timepit.refined.auto.autoUnwrap
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`

package object routes {
  def getFileType(filename: FileName): String = filename.value.drop(filename.lastIndexOf(".") + 1)

  def filePath(fileId: String): FilePath = FilePath.unsafeFrom(fileId)

  def genFileKey[F[_]: Sync](orgFilename: FileName): F[FileKey] =
    GenUUID[F].make.map { uuid =>
      FileKey.unsafeFrom(uuid.toString + "." + getFileType(orgFilename))
    }

  def nameToContentType(filename: FileName): Option[`Content-Type`] =
    MediaType
      .forExtension(filename.value.substring(filename.lastIndexOf('.') + 1))
      .map(`Content-Type`(_))

}
