package workout.stub_services

import com.itforelead.workout.domain.{Member, Payment}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithMember}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.Payments

class PaymentsStub[F[_]] extends Payments[F] {
  override def create(payment: CreatePayment): F[Payment] = ???
  override def payments(userId: UserId): F[List[PaymentWithMember]] = ???
  override def findExpireDateShort: F[List[Member]] = ???
}
