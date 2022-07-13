package workout.stub_services

import com.itforelead.workout.domain.Arrival.{ArrivalWithMember, CreateArrival}
import com.itforelead.workout.domain.Arrival
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.ArrivalService

class ArrivalStub[F[_]] extends ArrivalService[F] {
  override def create(userId: UserId, createArrival: CreateArrival): F[Arrival] = ???
  override def get(userId: UserId): F[List[ArrivalWithMember]] = ???
  override def getArrivalWithTotal(userId: UserId, page: Int): F[Arrival.ArrivalWithTotal] = ???
}
