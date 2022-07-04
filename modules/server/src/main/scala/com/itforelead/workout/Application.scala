package com.itforelead.workout

import cats.effect.std.Supervisor
import cats.effect.{IO, IOApp, Resource}
import com.itforelead.workout.config.ConfigLoader
import com.itforelead.workout.modules.Services
import com.itforelead.workout.resources.MkHttpServer
import dev.profunktor.redis4cats.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import skunk.Session

object Application extends IOApp.Simple {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigLoader.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { implicit sp =>
          resources
            .AppResources[IO](cfg)
            .evalMap { res =>
              implicit val session: Resource[IO, Session[IO]] = res.postgres

              val services = Services[IO](cfg.messageBroker, cfg.scheduler, res.httpClient, res.redis)
              services.notificationMessage.start >>
                modules.Security[IO](cfg, services.users, res.redis).map { security =>
                  cfg.serverConfig -> modules.HttpApi[IO](security, services, res.redis, cfg.logConfig).httpApp
                }
            }
            .flatMap { case (cfg, httpApp) =>
              MkHttpServer[IO].newEmber(cfg, httpApp)
            }
            .useForever
        }
    }

}
