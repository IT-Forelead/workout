package workout.utils

import cats.effect.std.{Console, Supervisor}
import cats.effect.{Async, IO, Resource}
import cats.implicits._
import ciris.Secret
import com.itforelead.workout.config.{AppConfig, ConfigLoader}
import com.itforelead.workout.domain
import com.itforelead.workout.domain.AppEnv.TEST
import com.itforelead.workout.domain.custom.refinements.{Password, Tel}
import com.itforelead.workout.effects.Background
import com.itforelead.workout.modules.{HttpApi, Security, Services}
import com.itforelead.workout.resources.AppResources
import com.itforelead.workout.routes.deriveEntityEncoder
import com.itforelead.workout.services.redis.RedisClient
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import eu.timepit.refined.cats.refTypeShow
import eu.timepit.refined.types.all.NonSystemPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Status, _}
import org.typelevel.log4cats.Logger
import skunk.Session
import weaver.scalacheck.{CheckConfig, Checkers}
import weaver.{Expectations, IOSuite}

trait ClientSuite extends IOSuite with Checkers with Container {
  case class Resources[F[_]](client: Client[F], redis: RedisClient[F])
  type Res = Resources[IO]
  override def checkConfig: CheckConfig = customCheckConfig

  val tel: Tel           = Tel.unsafeFrom("+998901234567")
  val password: Password = Password.unsafeFrom("Secret1!")

  def application[F[_]: Async: Logger: Console](
    config: AppConfig
  )(implicit ev: Background[F]): Resource[F, (HttpApp[F], RedisClient[F])] =
    AppResources[F](config)
      .evalMap { res =>
        implicit val session: Resource[F, Session[F]] = res.postgres

        val services = Services[F](config.messageBroker, config.scheduler, res.httpClient, res.redis)
        Security[F](config, services.users, res.redis).map { security =>
          HttpApi[F](security, services, res.s3Client, res.redis, config.logConfig).httpApp -> res.redis
        }
      }

  private def httpAppRes: Resource[IO, (HttpApp[IO], RedisClient[IO])] = {
    for {
      container <- dbResource

      httpApp <- Supervisor[IO].flatMap { implicit sp =>
        Resource.eval(ConfigLoader.load[IO]).flatMap { cfg =>
          application {
            if (cfg.env == TEST)
              cfg.copy(dbConfig =
                cfg.dbConfig.copy(
                  host = NonEmptyString.unsafeFrom(container.getHost),
                  port = NonSystemPortNumber.unsafeFrom(container.getFirstMappedPort),
                  user = NonEmptyString.unsafeFrom(container.getUsername),
                  password = Secret(NonEmptyString.unsafeFrom(container.getPassword)),
                  database = NonEmptyString.unsafeFrom(container.getDatabaseName)
                )
              )
            else cfg
          }
        }
      }
    } yield httpApp

  }
  def loginReq: Request[IO] =
    POST(domain.Credentials(tel, password), uri"/auth/login")

  def makeAuth: JwtToken => Authorization = token => Authorization(Credentials.Token(AuthScheme.Bearer, token.value))

  override def sharedResource: Resource[IO, Res] =
    httpAppRes.map { case httpApp -> redis =>
      Resources(Client.fromHttpApp[IO](httpApp), redis)
    }

  implicit class RequestOps[F[_]: Async](request: Request[F]) {
    def expectHttpStatus(status: Status)(implicit
      res: Resources[F]
    ): F[Expectations] =
      res.client.status(request).map { found =>
        expect.same(found, status)
      }

    def expectAs[A](implicit res: Resources[F], ev: EntityDecoder[F, A]): F[A] =
      res.client.expect[A](request)
  }

}
