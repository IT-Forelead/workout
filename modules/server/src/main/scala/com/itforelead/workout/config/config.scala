package com.itforelead.workout

import ciris.ConfigDecoder
import org.http4s.Uri

package object config {

  implicit val UriConfigDecoder: ConfigDecoder[String, Uri] =
    ConfigDecoder[String].mapOption("BigInt") { uri =>
      Uri.fromString(uri).toOption
    }
}
