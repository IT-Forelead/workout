package com.itforelead.workout.services

import cats.effect._
import cats.implicits._
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.refinements.{Tel, ValidationCode}
import com.itforelead.workout.domain.{Message, Validation}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.redis.RedisClient
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Session

import scala.concurrent.duration.DurationInt

trait Validations[F[_]] {
  def sendValidationCode(phone: Tel): F[Unit]
  def validatePhone(validation: Validation): F[Boolean]
}

object Validations {

  def apply[F[_]: GenUUID: Sync](messageBroker: MessageBroker[F], members: Members[F], redis: RedisClient[F])(implicit
    session: Resource[F, Session[F]],
    F: Async[F]
  ): Validations[F] =
    new Validations[F] with SkunkHelper[F] {

      def sendValidationCode(phone: Tel): F[Unit] = {
        val validationCode = scala.util.Random.between(10000, 99999)
        redis.put(phone.value, validationCode.toString, 1 minute)
        val messageText = NonEmptyString.unsafeFrom(s"Your Activation code is $validationCode")
        messageBroker.sendSMS(Message(phone, messageText))
      }

      def validatePhone(validation: Validation): F[Boolean] =
        for {
          redisCode <- redis.get(validation.phone.value)
          bool = redisCode.fold(false)(_ == validation.code.value)
          _ <-
            if (bool) { members.create(validation.memberParams) }
            else { F.unit }
        } yield bool

    }
}
