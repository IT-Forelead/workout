package workout.utils

import cats.effect.{IO, Resource}
import com.itforelead.workout.services.redis.RedisClient
import natchez.Trace.Implicits.noop
import skunk._
import skunk.codec.all.text
import skunk.implicits._
import skunk.util.Typer
import weaver.scalacheck.{CheckConfig, Checkers}
import weaver.{Expectations, IOSuite}
import workout.stub_services.RedisClientMock

trait DBSuite extends IOSuite with Checkers with Container {
  type Res = Resource[IO, Session[IO]]

  val RedisClient: RedisClient[IO] = RedisClientMock[IO]

  override def checkConfig: CheckConfig = CheckConfig.default.copy(minimumSuccessful = 1)

  def testBeforeAfterEach(
    before: Res => IO[Unit],
    after: Res => IO[Unit]
  ): String => (Res => IO[Expectations]) => Unit =
    name => fa => test(name)(res => before(res) >> fa(res).guarantee(after(res)))

  def testAfterEach(after: Res => IO[Unit]): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(_ => IO.unit, after)

  def testBeforeEach(before: Res => IO[Unit]): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(before, _ => IO.unit)

  def checkPostgresConnection(
    postgres: Resource[IO, Session[IO]]
  ): IO[Unit] =
    postgres.use { session =>
      session.unique(sql"select version();".query(text)).flatMap { v =>
        logger.info(s"Connected to Postgres $v")
      }
    }

  override def sharedResource: Resource[IO, Res] =
    for {
      container <- dbResource
      session <- Session
        .pooled[IO](
          host = container.getHost,
          port = container.getFirstMappedPort,
          user = container.getUsername,
          password = Some(container.getPassword),
          database = container.getDatabaseName,
          max = 100,
          strategy = Typer.Strategy.SearchPath
        )
        .evalTap(checkPostgresConnection)
    } yield session
}
