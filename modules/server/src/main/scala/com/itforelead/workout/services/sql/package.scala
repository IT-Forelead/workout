package com.itforelead.workout.services

import com.itforelead.workout.domain.custom.refinements.{EmailAddress, FilePath, FullName, Tel, UrlAddress}
import com.itforelead.workout.domain.types._
import com.itforelead.workout.domain.Role
import com.itforelead.workout.types.IsUUID
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Codec
import skunk.codec.all._
import skunk.data.{Arr, Type}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.string.Url

import java.util.UUID
import scala.util.Try

package object sql {

  def parseUUID: String => Either[String, UUID] = s =>
    Try(Right(UUID.fromString(s))).getOrElse(Left(s"Invalid argument: [ $s ]"))

  val _uuid: Codec[Arr[UUID]] = Codec.array(_.toString, parseUUID, Type._uuid)

  val listUUID: Codec[List[UUID]] = _uuid.imap(_.flattenTo(List))(l => Arr(l: _*))

  def identity[A: IsUUID]: Codec[A] = uuid.imap[A](IsUUID[A]._UUID.get)(IsUUID[A]._UUID.apply)

  val userName: Codec[UserName] = varchar.imap[UserName](name => UserName(FullName.unsafeFrom(name)))(_.value)

  val passwordHash: Codec[PasswordHash[SCrypt]] = varchar.imap[PasswordHash[SCrypt]](PasswordHash[SCrypt])(_.toString)

  val tel: Codec[Tel] = varchar.imap[Tel](Tel.unsafeFrom)(_.value)

  val url: Codec[UrlAddress] = varchar.imap[UrlAddress](UrlAddress.unsafeFrom)(_.value)

  val role: Codec[Role] = `enum`[Role](_.value, Role.find, Type("role"))

  val filePath: Codec[FilePath] = varchar.imap[FilePath](FilePath.unsafeFrom)(_.value)
}
