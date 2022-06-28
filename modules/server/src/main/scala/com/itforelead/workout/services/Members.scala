package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithPassword}
import com.itforelead.workout.domain.{ID, Member, Message, Validation}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MemberSQL.{insertMember, selectMember}
import eu.timepit.refined.types.string.NonEmptyString
import skunk.{Session, ~}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

trait Members[F[_]] {
  def find(phone: Tel): F[Option[MemberWithPassword]]
  def create(memberParam: CreateMember, password: PasswordHash[SCrypt]): F[Member]
}

object Members {

  def apply[F[_]: GenUUID: Sync](messageBroker: MessageBroker[F])(implicit
    session: Resource[F, Session[F]]
  ): Members[F] =
    new Members[F] with SkunkHelper[F] {

      def find(phone: Tel): F[Option[MemberWithPassword]] =
        OptionT(prepOptQuery(selectMember, phone)).map { case member ~ p =>
          MemberWithPassword(member, p)
        }.value

      def create(memberParam: CreateMember, password: PasswordHash[SCrypt]): F[Member] = {
        messageBroker.sendSMS(
          Message(
            memberParam.phone,
            NonEmptyString.unsafeFrom("Your Activation code is [Activation Code]")
          )
        )

        ID.make[F, memberId]
          .flatMap { id =>
            prepQueryUnique(insertMember, id ~ memberParam ~ password).map(_._1)
          }
      }
    }

}
