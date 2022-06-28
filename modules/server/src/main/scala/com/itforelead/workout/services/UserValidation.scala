package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import cats.implicits.toFunctorOps
import com.itforelead.workout.domain.Member.Validation
import com.itforelead.workout.domain.Message
import com.itforelead.workout.domain.custom.refinements.{Tel, ValidationCode}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.implicits.CirceDecoderOps
import com.itforelead.workout.services.redis.RedisClient
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Session

import scala.concurrent.duration.DurationInt

trait UserValidation[F[_]] {
  def sendValidationCode(phone: Tel): F[Unit]
  def validatePhone(validation: Validation): F[Boolean]
}

object UserValidation {

  def apply[F[_]: GenUUID: Sync](messageBroker: MessageBroker[F], redis: RedisClient[F])(implicit
    session: Resource[F, Session[F]]
  ): UserValidation[F] =
    new UserValidation[F] with SkunkHelper[F] {

      def sendValidationCode(phone: Tel): F[Unit] = {
        val validationCode = scala.util.Random.between(10000, 99999)
        redis.put(phone.value, Validation(phone, ValidationCode.unsafeFrom(validationCode.toString)), 1 minute)
        val messageText = NonEmptyString.unsafeFrom(s"Your Activation code is $validationCode")
        messageBroker.sendSMS(Message(phone, messageText))
      }

      def validatePhone(validation: Validation): F[Boolean] = {
        val check = redis.get(validation.phone.value)
        check.map(m =>
          m.get.as[Validation].phone.value == validation.phone.value &&
            m.get.as[Validation].code.value == validation.code.value
        )
      }

    }
}
