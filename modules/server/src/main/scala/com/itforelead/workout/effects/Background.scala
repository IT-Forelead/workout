package com.itforelead.workout.effects

import scala.concurrent.duration.FiniteDuration

import cats.effect._
import cats.effect.std.Supervisor
import cats.syntax.all._

trait Background[F[_]] {
  def scheduleOnce[A](fa: F[A], duration: FiniteDuration): F[Unit]
  def schedule[A](
      fa: F[A],
      duration: FiniteDuration,
      interval: FiniteDuration,
    ): F[Unit]
}

object Background {
  def apply[F[_]: Background]: Background[F] = implicitly

  implicit def bgInstance[F[_]](implicit S: Supervisor[F], T: Temporal[F]): Background[F] =
    new Background[F] {
      private def retry[A](delay: FiniteDuration, fa: F[A]): F[Unit] =
        scheduleOnce(fa >> retry(delay, fa), delay)

      def scheduleOnce[A](fa: F[A], delay: FiniteDuration): F[Unit] =
        S.supervise(T.delayBy(fa, delay)).void

      def schedule[A](
          fa: F[A],
          delay: FiniteDuration,
          interval: FiniteDuration,
        ): F[Unit] =
        scheduleOnce(fa >> retry(interval, fa), delay)
    }
}
