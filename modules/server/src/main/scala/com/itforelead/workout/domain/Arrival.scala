package com.itforelead.workout.domain

import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

import java.time.LocalDateTime

@derive(decoder, encoder, show)
case class Arrival(
  id: ArrivalId,
  userId: UserId,
  memberId: MemberId,
  createdAt: LocalDateTime,
  arrivalType: ArrivalType
)

object Arrival {

  @derive(decoder, encoder, show)
  case class CreateArrival(
    userId: UserId,
    memberId: MemberId,
    createdAt: LocalDateTime,
    arrivalType: ArrivalType
  )
}
