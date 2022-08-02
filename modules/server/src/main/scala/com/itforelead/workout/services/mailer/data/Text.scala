package com.itforelead.workout.services.mailer.data

import com.itforelead.workout.services.mailer.data.types.Subtype
import com.itforelead.workout.services.mailer.data.types.Subtype.PLAIN

import java.nio.charset.{ Charset, StandardCharsets }

case class Text(
    value: String,
    charset: Charset = StandardCharsets.UTF_8,
    subtype: Subtype = PLAIN,
    headers: List[Header] = Nil,
  )
