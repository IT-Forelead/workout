package com.itforelead.workout.resources

import cats.effect.kernel.{Async, Resource}
import com.comcast.ip4s.{Host, Port}
import com.itforelead.workout.config.HttpServerConfig
import eu.timepit.refined.auto.autoUnwrap
import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.Logger

trait MkHttpServer[F[_]] {
  def newEmber(cfg: HttpServerConfig, httpApp: HttpApp[F]): Resource[F, Server]
}

object MkHttpServer {
  def apply[F[_]: MkHttpServer]: MkHttpServer[F] = implicitly
  val Banner: String =
    """
      |        c.  .l                       c'  .c
      |     ;o.      ;       Ko:;cdX       ,      .o;
      |    l'  ,XNWX '     ,'       ,,     . KWNX;  'l
      |  .d   l.           :.       .;           .c   d.
      | ,,    ..          . .         .          ..    ,,
      | ,     '.            ;       :            .'     ,
      |:      ' .;lOWK0K0    c     c    kKKKNOl;; .      :
      |.;                .lk         ko.                :.
      |  .x:                                         ;x.
      |     c0;                                   ,0c
      |        .clolx                       xlolc.
      |              l                     c
      |               d'                 'd
      |                .;               ,.
      |                 .:             ;.
      |                  _ _ - _ _ - _ _
      |      __      _____  _ __| | _____  _   _| |_
      |      \ \ /\ / / _ \| '__| |/ / _ \| | | | __|
      |       \ V  V / (_) | |  |   < (_) | |_| | |_
      |        \_/\_/ \___/|_|  |_|\_\___/ \__,_|\__|
      |""".stripMargin
  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(s"\n$Banner\nHTTP Server started at ${s.address}")

  implicit def forAsyncLogger[F[_]: Async: Logger]: MkHttpServer[F] =
    (cfg: HttpServerConfig, httpApp: HttpApp[F]) =>
      EmberServerBuilder
        .default[F]
        .withHostOption(Host.fromString(cfg.host))
        .withPort(Port.fromString(cfg.port.toString()).getOrElse(throw new Exception("")))
        .withHttpApp(httpApp)
        .build
        .evalTap(showEmberBanner[F])
}
