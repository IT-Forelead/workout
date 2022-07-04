package com.itforelead.workout.services

import cats.effect._
import cats.implicits._
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.{Member, Message}
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeExpired}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.redis.RedisClient
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Session

import scala.concurrent.duration.DurationInt

trait Validations[F[_]] {
  def sendValidationCode(phone: Tel): F[Unit]
  def validatePhone(createMember: CreateMember): F[Boolean]
}

object Validations {

  def apply[F[_]: GenUUID: Sync](messageBroker: MessageBroker[F], members: Members[F], redis: RedisClient[F])(implicit
    session: Resource[F, Session[F]],
    F: Async[F]
  ): Validations[F] =
    new Validations[F] with SkunkHelper[F] {

      def sendValidationCode(phone: Tel): F[Unit] = {
        val validationCode = scala.util.Random.between(100000, 999999)
        redis.put(phone.value, validationCode.toString, 3 minute)
        val messageText = NonEmptyString.unsafeFrom(s"Your Activation code is $validationCode")
        messageBroker.sendSMS(Message(phone, messageText))
      }

      def validatePhone(createMember: CreateMember): F[Boolean] = {
        val members = Members[F]
        for {
          redisCode <- redis.get(createMember.phone.value)
          bool = redisCode match {
            case Some(f) =>
              if (f == createMember.code.value) {
                if (members.findMemberByPhone(createMember.phone) == F[Member]) PhoneInUse(createMember.phone)
              }
            case None => ValidationCodeExpired(createMember.phone)
          }
        } yield bool
      }
    }
}
