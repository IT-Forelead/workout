package workout.http.routes

import cats.effect.{IO, Sync}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentMemberId, PaymentWithMember}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{Member, Payment, types}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{PaymentRoutes, deriveEntityEncoder}
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import workout.stub_services.PaymentsStub
import workout.utils.Generators._
import workout.utils.HttpSuite

object PaymentRoutesSuite extends HttpSuite {
  private def paymentS[F[_]: Sync: GenUUID](payment: Payment, member: Member): PaymentsStub[F] = new PaymentsStub[F] {
    override def create(userId: UserId, createPayment: CreatePayment): F[Payment] = Sync[F].delay(payment)
    override def payments(userId: UserId): F[List[PaymentWithMember]] =
      Sync[F].delay(List(PaymentWithMember(payment, member)))
    override def getPaymentByMemberId(userId: UserId, memberId: types.MemberId): F[List[Payment]] =
      Sync[F].delay(List(payment))
  }

  test("GET Payments") {
    val gen = for {
      u <- userGen
      m <- memberGen
      p <- paymentGen
    } yield (u, m, p)

    forall(gen) { case (user, member, payment) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/payment").putHeaders(token)
        routes = new PaymentRoutes[IO](paymentS(payment, member)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("GET Payments by MemberId") {
    val gen = for {
      u <- userGen
      m <- memberGen
      p <- paymentGen
      i <- paymentMemberIdGen
    } yield (u, m, p, i)

    forall(gen) { case (user, member, payment, memberId) =>
      for {
        token <- authToken(user)
        req    = POST(memberId, uri"/payment/member").putHeaders(token)
        routes = new PaymentRoutes[IO](paymentS(payment, member)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("POST Payment") {
    val gen = for {
      u  <- userGen
      m  <- memberGen
      cp <- createPaymentGen
      p  <- paymentGen
    } yield (u, m, cp, p)

    forall(gen) { case (user, member, createPay, payment) =>
      for {
        token <- authToken(user)
        req    = POST(createPay, uri"/payment").putHeaders(token)
        routes = new PaymentRoutes[IO](paymentS(payment, member)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Created)
      } yield res
    }
  }
}
