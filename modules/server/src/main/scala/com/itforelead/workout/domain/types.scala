package com.itforelead.workout.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.estatico.newtype.macros.newtype
import com.itforelead.workout.types.uuid
import io.circe.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.numeric.NonNegShort
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Codec
import skunk.data.Type
import skunk.codec.all._
import squants.market.Currency

import java.util.UUID
import javax.crypto.Cipher

object types {

  object UZS extends Currency("UZS", "Uzbek sum", "SUM", 2)

  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class UserId(value: UUID)

  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class MemberId(value: UUID)

  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class ArrivalId(value: UUID)

  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class MessageId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype case class FirstName(value: NonEmptyString)

  @derive(decoder, encoder, eqv, show)
  @newtype case class LastName(value: NonEmptyString)

  @derive(decoder, encoder, eqv, show)
  @newtype case class GymName(value: NonEmptyString)

  @derive(decoder, encoder, eqv, show)
  @newtype case class MessageText(value: NonEmptyString)

  @derive(decoder, encoder, eqv, show)
  @newtype case class Duration(value: NonNegShort)

  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class PaymentId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype case class EncryptedPassword(value: String)

  val arrivalType: Codec[ArrivalType] =
    `enum`[ArrivalType](_.value, ArrivalType.find, Type("arrival_type"))

  @newtype case class EncryptCipher(value: Cipher)

  @newtype case class DecryptCipher(value: Cipher)

  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

}
