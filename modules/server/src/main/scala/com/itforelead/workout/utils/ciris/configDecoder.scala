package com.itforelead.workout.utils.ciris

import _root_.ciris.ConfigDecoder
import com.itforelead.workout.utils.derevo.Derive

object configDecoder extends Derive[Decoder.Id]

object Decoder {
  type Id[A] = ConfigDecoder[String, A]
}
