package workout.stub_services

import cats.effect.Sync
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import com.itforelead.workout.implicits.GenericTypeOps
import com.itforelead.workout.services.redis.RedisClient
import io.circe.Encoder

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object RedisClientMock {
  val Redis = mutable.HashMap.empty[String, String]

  def apply[F[_]: Sync]: RedisClient[F] = new RedisClient[F] {
    override def put(
      key: String,
      value: String,
      expire: FiniteDuration
    ): F[Unit] = Redis.put(key, value).pure[F].void

    override def put[T: Encoder](
      key: String,
      value: T,
      expire: FiniteDuration
    ): F[Unit] = Sync[F].delay(Redis.put(key, value.toJson)).void

    override def get(key: String): F[Option[String]] = Sync[F].delay(Redis.get(key))

    override def del(key: String*): F[Unit] = key.foreach(Redis.remove).pure[F]
  }
}
