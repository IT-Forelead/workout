package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Arrival.{ArrivalFilter, ArrivalWithMember, ArrivalWithTotal, CreateArrival}
import com.itforelead.workout.domain.custom.exception.MemberNotFound
import com.itforelead.workout.domain.{Arrival, ID}
import com.itforelead.workout.domain.types.{ArrivalId, MemberId, UserId}
import com.itforelead.workout.services.sql.ArrivalSQL._
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.ArrivalSQL
import skunk.codec.all.int8
import skunk.implicits.toIdOps
import skunk.{Session, SqlState}

import java.time.LocalDateTime

trait ArrivalService[F[_]] {
  def create(userId: UserId, form: CreateArrival): F[Arrival]
  def get(userId: UserId): F[List[ArrivalWithMember]]
  def getArrivalByMemberId(userId: UserId, memberId: MemberId): F[List[Arrival]]
  def getArrivalWithTotal(userId: UserId, filter: ArrivalFilter, page: Int): F[ArrivalWithTotal]
}

object ArrivalService {

  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): ArrivalService[F] =
    new ArrivalService[F] with SkunkHelper[F] {

      override def create(userId: UserId, form: CreateArrival): F[Arrival] =
        for {
          id  <- ID.make[F, ArrivalId]
          now <- Sync[F].delay(LocalDateTime.now())
          arrival <- prepQueryUnique(
            insertSql,
            Arrival(id, userId, form.memberId, now, form.arrivalType)
          ).recoverWith { case SqlState.ForeignKeyViolation(_) =>
            MemberNotFound.raiseError[F, Arrival]
          }
        } yield arrival

      override def get(userId: UserId): F[List[ArrivalWithMember]] =
        prepQueryList(selectSql, userId)

      override def getArrivalByMemberId(userId: UserId, memberId: MemberId): F[List[Arrival]] =
        prepQueryList(selectArrivalByMemberId, userId ~ memberId)

      override def getArrivalWithTotal(userId: UserId, filter: ArrivalFilter, page: Int): F[ArrivalWithTotal] =
        for {
          fr      <- selectArrivalWithTotal(userId, filter, page).pure[F]
          t       <- total(userId, filter).pure[F]
          arrival <- prepQueryList(fr.fragment.query(ArrivalSQL.decArrivalWithMember), fr.argument)
          total   <- prepQueryUnique(t.fragment.query(int8), t.argument)
        } yield ArrivalWithTotal(arrival, total)

    }
}
