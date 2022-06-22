package com.itforelead.workout.modules

import cats.effect.{Resource, Sync}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services._
import org.typelevel.log4cats.Logger
import skunk.Session

object Services {
  def apply[F[_]: Sync: GenUUID: Logger](implicit session: Resource[F, Session[F]]): Services[F] = {
    new Services[F](
      users = Users[F],
      payments = Payments[F]
    )
  }
}

final class Services[F[_]] private (
  val users: Users[F],
  val payments: Payments[F]
)
