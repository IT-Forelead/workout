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
    memberId: MemberId,
    arrivalType: ArrivalType
  )

  @derive(decoder, encoder, show)
  case class ArrivalWithMember(
    arrival: Arrival,
    member: Member
  )

  @derive(decoder, encoder, show)
  case class ArrivalWithTotal(
    arrival: List[ArrivalWithMember],
    total: Long
  )

  @derive(decoder, encoder, show)
  case class ArrivalFilter(
    typeBy: Option[ArrivalType] = None,
    filterDateFrom: Option[LocalDateTime] = None,
    filterDateTo: Option[LocalDateTime] = None
  )

  @derive(decoder, encoder, show)
  case class ArrivalMemberId(memberId: MemberId)
}
