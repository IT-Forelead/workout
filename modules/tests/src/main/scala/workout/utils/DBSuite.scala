package workout.utils

import cats.effect.{IO, Resource}
import cats.implicits._
import com.itforelead.workout.services.redis.RedisClient
import natchez.Trace.Implicits.noop
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk._
import skunk.codec.all.text
import skunk.implicits._
import skunk.util.Typer
import weaver.scalacheck.{CheckConfig, Checkers}
import weaver.{Expectations, IOSuite}
import workout.stub_services.RedisClientMock

import scala.io.Source

trait DBSuite extends IOSuite with Checkers {
  type Res = Resource[IO, Session[IO]]

  val RedisClient: RedisClient[IO] = RedisClientMock[IO]

  lazy val imageName: String = "postgres:12"
  lazy val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer(
    DockerImageName
      .parse(imageName)
      .asCompatibleSubstituteFor("postgres")
  )

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def checkConfig: CheckConfig = CheckConfig.default.copy(minimumSuccessful = 1)

  implicit class SharedResOps(res: Resource[IO, Res]) {
    def beforeAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.evalTap(f)

    def afterAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.flatTap(x => Resource.make(IO.unit)(_ => f(x)))
  }

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

  def migrateSql(container: PostgreSQLContainer[Nothing]): Unit = {
    val source     = Source.fromFile(getClass.getResource("/tables.sql").getFile)
    val sqlScripts = source.getLines().mkString
    source.close()
    val connection = container.createConnection(s"?user=${container.getUsername}&password=${container.getPassword}")
    val stmt       = connection.createStatement()
    stmt.execute(sqlScripts)
    stmt.closeOnCompletion()
  }

  override def sharedResource: Resource[IO, Res] =
    for {
      container <- Resource.fromAutoCloseable {
        IO {
          container.start()
          container
        }
      }
      _ = migrateSql(container)
      _ <- Resource.eval(logger.info("Container has started"))
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
