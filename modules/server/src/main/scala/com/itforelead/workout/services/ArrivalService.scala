package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Arrival.{ArrivalWithMember, ArrivalWithTotal, CreateArrival}
import com.itforelead.workout.domain.{Arrival, ID}
import com.itforelead.workout.domain.types.{ArrivalId, UserId}
import com.itforelead.workout.services.sql.ArrivalSQL._
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.ArrivalSQL
import skunk.Session

import java.time.LocalDateTime

trait ArrivalService[F[_]] {
  def create(userId: UserId, form: CreateArrival): F[Arrival]
  def get(userId: UserId): F[List[ArrivalWithMember]]
  def getArrivalWithTotal(userId: UserId, page: Int): F[ArrivalWithTotal]
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
          )
        } yield arrival

      override def get(userId: UserId): F[List[ArrivalWithMember]] =
        prepQueryList(selectSql, userId)

      override def getArrivalWithTotal(userId: UserId, page: Int): F[ArrivalWithTotal] =
        for {
          fr       <- selectArrivalWithTotal(userId, page).pure[F]
          messages <- prepQueryList(fr.fragment.query(ArrivalSQL.decArrivalWithMember), fr.argument)
          total    <- prepQueryUnique(total, userId)
        } yield ArrivalWithTotal(messages, total)

    }
}
