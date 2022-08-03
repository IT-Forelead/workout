package com.itforelead.workout

import cats.implicits.toBifunctorOps
import ciris.{ ConfigDecoder, ConfigError }
import com.itforelead.workout.domain.AppEnv
import com.itforelead.workout.domain.custom.refinements.EmailAddress
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.refined._
import org.http4s.Uri

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import scala.util.Try

package object config {
  implicit val UriConfigDecoder: ConfigDecoder[String, Uri] =
    ConfigDecoder[String].mapOption("Uri") { uri =>
      Uri.fromString(uri).toOption
    }

  implicit val AppEnvConfigDecoder: ConfigDecoder[String, AppEnv] =
    ConfigDecoder[String].mapOption("AppEnv") { env =>
      AppEnv.find(env)
    }

  implicit val LocalTimeConfigDecoder: ConfigDecoder[String, LocalTime] =
    ConfigDecoder[String].mapOption("LocalTime") { time =>
      Try(LocalTime.parse(time, DateTimeFormatter.ofPattern("h:mm a"))).toOption
    }

  def circeConfigDecoder[A: Decoder]: ConfigDecoder[String, A] =
    ConfigDecoder[String].mapEither { (_, s) =>
      decode[A](s).leftMap(error => ConfigError(error.getMessage))
    }

  implicit val emailAddressesDecoder: ConfigDecoder[String, List[EmailAddress]] =
    circeConfigDecoder[List[EmailAddress]]
}
