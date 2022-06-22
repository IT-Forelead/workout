package workout.stub_services

import com.itforelead.workout.domain.Payment
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithUser}
import com.itforelead.workout.services.Payments


class PaymentsStub[F[_]] extends Payments[F] {
  override def create(payment: CreatePayment): F[Payment] = ???
  override def payments: F[List[PaymentWithUser]] = ???
}
