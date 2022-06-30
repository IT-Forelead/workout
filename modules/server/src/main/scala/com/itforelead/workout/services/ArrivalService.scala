package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Arrival.CreateArrival
import com.itforelead.workout.domain.{Arrival, ID}
import com.itforelead.workout.domain.types.{ArrivalId, UserId}
import com.itforelead.workout.services.sql.ArrivalSQL._
import com.itforelead.workout.effects.GenUUID
import skunk.Session

import java.time.LocalDateTime

trait ArrivalService[F[_]] {
  def create(form: CreateArrival): F[Arrival]
  def get(userId: UserId): F[List[Arrival]]
}

object ArrivalService {

  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): ArrivalService[F] =
    new ArrivalService[F] with SkunkHelper[F] {

      override def create(form: CreateArrival): F[Arrival] =
        for {
          id  <- ID.make[F, ArrivalId]
          now <- Sync[F].delay(LocalDateTime.now())
          arrival <- prepQueryUnique(
            insertSql,
            Arrival(id, form.userId, form.memberId, now, form.arrivalType)
          )
        } yield arrival

      override def get(userId: UserId): F[List[Arrival]] =
        prepQueryList(selectSql, userId)

    }
}
