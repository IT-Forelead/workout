package com.itforelead.workout.services.mailer.data

import com.itforelead.workout.services.mailer.data.types.Subtype
import com.itforelead.workout.services.mailer.data.types.Subtype.HTML

import java.nio.charset.{ Charset, StandardCharsets }

case class Html(
    value: String,
    charset: Charset = StandardCharsets.UTF_8,
    subtype: Subtype = HTML,
    headers: List[Header] = Nil,
  )
