package com.itforelead.workout.services.mailer.data

import io.estatico.newtype.macros.newtype

object types {
  @newtype case class EmailAddress(value: String)
  @newtype case class Subtype(value: String)
  @newtype case class Host(value: String)
  @newtype case class Port(value: Int)
  @newtype case class Protocol(value: String)
  @newtype case class Password(value: String)

  object Subtype {
    val HTML: Subtype = Subtype("html")
    val PLAIN: Subtype = Subtype("plain")
  }
  object Protocol {
    val Smtp: Protocol = Protocol("smtp")
    val Imap: Protocol = Protocol("imap")
  }
}
