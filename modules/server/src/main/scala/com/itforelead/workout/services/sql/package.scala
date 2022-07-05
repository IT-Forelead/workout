package com.itforelead.workout.services

import cats.implicits._
import com.itforelead.workout.domain.{DeliveryStatus, PaymentType}
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.domain.types._
import com.itforelead.workout.types.IsUUID
import skunk._
import skunk.codec.all._
import skunk.data.{Arr, Type}
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.types.numeric.NonNegShort
import eu.timepit.refined.types.string.NonEmptyString
import squants.Money

import java.util.UUID
import scala.util.Try

package object sql {

  def parseUUID: String => Either[String, UUID] = s =>
    Try(Right(UUID.fromString(s))).getOrElse(Left(s"Invalid argument: [ $s ]"))

  val _uuid: Codec[Arr[UUID]] = Codec.array(_.toString, parseUUID, Type._uuid)

  val listUUID: Codec[List[UUID]] = _uuid.imap(_.flattenTo(List))(l => Arr(l: _*))

  def identity[A: IsUUID]: Codec[A] = uuid.imap[A](IsUUID[A]._UUID.get)(IsUUID[A]._UUID.apply)

  val firstName: Codec[FirstName] = varchar.imap[FirstName](name => FirstName(NonEmptyString.unsafeFrom(name)))(_.value)

  val lastName: Codec[LastName] = varchar.imap[LastName](name => LastName(NonEmptyString.unsafeFrom(name)))(_.value)

  val text: Codec[Text] = varchar.imap[Text](name => Text(NonEmptyString.unsafeFrom(name)))(_.value)

  val gymName: Codec[GymName] = varchar.imap[GymName](name => GymName(NonEmptyString.unsafeFrom(name)))(_.value)

  val passwordHash: Codec[PasswordHash[SCrypt]] = varchar.imap[PasswordHash[SCrypt]](PasswordHash[SCrypt])(_.toString)

  val tel: Codec[Tel] = varchar.imap[Tel](Tel.unsafeFrom)(_.value)

  val paymentType: Codec[PaymentType] = `enum`[PaymentType](_.value, PaymentType.find, Type("payment_type"))

  val deliveryStatus: Codec[DeliveryStatus] =
    `enum`[DeliveryStatus](_.value, DeliveryStatus.find, Type("delivery_status"))

  val price: Codec[Money] = numeric.imap[Money](money => UZS(money))(_.amount)

  val duration: Codec[Duration] = int2.imap[Duration](duration => Duration(NonNegShort.unsafeFrom(duration)))(_.value)

  val fileKey: Codec[FileKey] = varchar.imap[FileKey](fileKey => FileKey.unsafeFrom(fileKey))(_.value)

  final implicit class FragmentOps(af: AppliedFragment) {
    def paginate(lim: Int, index: Int): AppliedFragment = {
      val offset                      = (index - 1) * lim
      val filter: Fragment[Int ~ Int] = sql"LIMIT $int4 OFFSET $int4"
      af |+| filter(lim ~ offset)
    }

    /** Returns `WHERE (f1) AND (f2) AND ... (fn)` for defined `f`, if any, otherwise the empty fragment. */
    def whereAndOpt(fs: AppliedFragment*): AppliedFragment = {
      val filters =
        if (fs.toList.isEmpty)
          AppliedFragment.empty
        else
          fs.foldSmash(void" WHERE ", void" AND ", AppliedFragment.empty)
      af |+| filters
    }

    def whereAndOpt(fs: List[AppliedFragment]): AppliedFragment = {
      val filters =
        if (fs.isEmpty)
          AppliedFragment.empty
        else
          fs.foldSmash(void" WHERE ", void" AND ", AppliedFragment.empty)
      af |+| filters
    }

    def innerAndOpt(fs: List[AppliedFragment]): AppliedFragment = {
      val filters =
        if (fs.isEmpty)
          AppliedFragment.empty
        else
          fs.foldSmash(void" AND ", void" AND ", AppliedFragment.empty)
      af |+| filters
    }

  }

}
