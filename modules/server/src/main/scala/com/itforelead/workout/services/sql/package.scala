package com.itforelead.workout.services

import cats.implicits._
import com.itforelead.workout.domain.{ArrivalType, DeliveryStatus, PaymentType}
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

  def identity[A: IsUUID]: Codec[A] = uuid.imap[A](IsUUID[A]._UUID.get)(IsUUID[A]._UUID.apply)

  val nes: Codec[NonEmptyString] = varchar.imap[NonEmptyString](NonEmptyString.unsafeFrom)(_.value)

  val firstName: Codec[FirstName] = nes.imap[FirstName](FirstName.apply)(_.value)

  val lastName: Codec[LastName] = nes.imap[LastName](LastName.apply)(_.value)

  val messageText: Codec[MessageText] = nes.imap[MessageText](MessageText.apply)(_.value)

  val gymName: Codec[GymName] = nes.imap[GymName](GymName.apply)(_.value)

  val passwordHash: Codec[PasswordHash[SCrypt]] = varchar.imap[PasswordHash[SCrypt]](PasswordHash[SCrypt])(_.toString)

  val tel: Codec[Tel] = varchar.imap[Tel](Tel.unsafeFrom)(_.value)

  val paymentType: Codec[PaymentType] = `enum`[PaymentType](_.value, PaymentType.find, Type("payment_type"))

  val deliveryStatus: Codec[DeliveryStatus] =
    `enum`[DeliveryStatus](_.value, DeliveryStatus.find, Type("delivery_status"))

  val price: Codec[Money] = numeric.imap[Money](money => UZS(money))(_.amount)

  val duration: Codec[Duration] = int2.imap[Duration](duration => Duration(NonNegShort.unsafeFrom(duration)))(_.value)

  val fileKey: Codec[FileKey] = varchar.imap[FileKey](fileKey => FileKey.unsafeFrom(fileKey))(_.value)

  val arrivalType: Codec[ArrivalType] =
    `enum`[ArrivalType](_.value, ArrivalType.find, Type("arrival_type"))

  final implicit class FragmentOps(af: AppliedFragment) {
    def paginate(lim: Int, index: Int): AppliedFragment = {
      val offset                      = (index - 1) * lim
      val filter: Fragment[Int ~ Int] = sql" LIMIT $int4 OFFSET $int4 "
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
