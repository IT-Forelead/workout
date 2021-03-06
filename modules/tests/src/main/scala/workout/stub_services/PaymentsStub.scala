package workout.stub_services

import com.itforelead.workout.domain.{Member, Payment, types}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentFilter, PaymentWithMember}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.Payments

class PaymentsStub[F[_]] extends Payments[F] {
  override def create(userId: UserId, payment: CreatePayment): F[Payment] = ???
  override def payments(userId: UserId): F[List[PaymentWithMember]] = ???
  override def getPaymentsWithTotal(userId: UserId, filter: PaymentFilter, page: Int): F[Payment.PaymentWithTotal] = ???
  override def getPaymentByMemberId(userId: UserId, memberId: types.MemberId): F[List[Payment]] = ???
}
