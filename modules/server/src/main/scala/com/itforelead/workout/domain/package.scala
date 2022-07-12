package com.itforelead.workout

import cats.implicits.toContravariantOps
import cats.{Eq, Monoid, Show}
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.types.UZS
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.semiauto.{deriveCodec, deriveEncoder}
import io.circe.{Codec, Decoder, Encoder}
import squants.Money
import squants.market.Currency
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import io.circe.refined._

import java.time.{LocalDate, LocalDateTime}

package object domain {
  implicit val tokenEq: Eq[JwtToken] = Eq.by(_.value)

  implicit val tokenShow: Show[JwtToken] = Show[String].contramap[JwtToken](_.value)

  implicit val tokenCodec: Codec[JwtToken] = deriveCodec

  implicit val javaTimeShow: Show[LocalDateTime] = Show[String].contramap[LocalDateTime](_.toString)

  implicit val javaDateShow: Show[LocalDate] = Show[String].contramap[LocalDate](_.toString)

  implicit val passwordHashEncoder: Encoder[PasswordHash[SCrypt]] = Encoder.encodeString.contramap(_.toString)
  implicit val passwordHashDecoder: Decoder[PasswordHash[SCrypt]] = Decoder.decodeString.map(PasswordHash[SCrypt])

  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(UZS.apply)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val moneyMonoid: Monoid[Money] =
    new Monoid[Money] {
      def empty: Money                       = UZS(0)
      def combine(x: Money, y: Money): Money = x + y
    }

  implicit val currencyEq: Eq[Currency] = Eq.and(Eq.and(Eq.by(_.code), Eq.by(_.symbol)), Eq.by(_.name))

  implicit val moneyEq: Eq[Money] = Eq.and(Eq.by(_.amount), Eq.by(_.currency))

  implicit val moneyShow: Show[Money] = Show.fromToString

  implicit val encCreateMemberAsObject: Encoder.AsObject[CreateMember] = deriveEncoder[CreateMember]
}
