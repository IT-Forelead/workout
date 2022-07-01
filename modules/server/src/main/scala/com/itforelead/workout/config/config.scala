package com.itforelead.workout

import ciris.ConfigDecoder
import com.itforelead.workout.domain.AppEnv
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
}
