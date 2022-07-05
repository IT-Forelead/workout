package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect.{Async, Resource, Sync}
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxFlatMapOps, toFlatMapOps}
import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeError, ValidationCodeExpired}
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.redis.RedisClient
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Session

import scala.concurrent.duration.DurationInt

trait Validations[F[_]] {
  def sendValidationCode(phone: Tel): F[Unit]
  def validatePhone(createMember: CreateMember, key: FileKey): F[Member]
}

object Validations {

  def apply[F[_]: GenUUID: Sync](
    messageBroker: MessageBroker[F],
    members: Members[F],
    redis: RedisClient[F]
  )(implicit
    session: Resource[F, Session[F]],
    F: Async[F]
  ): Validations[F] =
    new Validations[F] with SkunkHelper[F] {

      def sendValidationCode(phone: Tel): F[Unit] = {
        val validationCode = scala.util.Random.between(100000, 999999)
        val messageText    = NonEmptyString.unsafeFrom(s"Your Activation code is $validationCode")
        redis.put(phone.value, validationCode.toString, 3.minute) >>
          messageBroker.sendSMSWithoutMember(phone, messageText)
      }

      override def validatePhone(createMember: CreateMember, key: FileKey): F[Member] = {
        OptionT(redis.get(createMember.phone.value))
          .cataF(
            ValidationCodeExpired(createMember.phone).raiseError[F, Member],
            code =>
              if (code == createMember.code.value) {
                members.findMemberByPhone(createMember.phone).flatMap {
                  case Some(_) => PhoneInUse(createMember.phone).raiseError[F, Member]
                  case None    => members.create(createMember, key)
                }
              } else
                ValidationCodeError(createMember.code).raiseError[F, Member]
          )
      }
    }
}
