package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.domain.{Member, Payment, User}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithMember}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{PaymentRoutes, deriveEntityEncoder}
import org.http4s.Method.{GET, POST}
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.Status
import workout.stub_services.PaymentsStub
import workout.utils.Generators._
import workout.utils.HttpSuite

object PaymentRoutesSuite extends HttpSuite {
  private def paymentS[F[_]: Sync: GenUUID](payment: Payment, member: Member): PaymentsStub[F] = new PaymentsStub[F] {
    override def create(createPayment: CreatePayment): F[Payment]     = Sync[F].delay(payment)
    override def payments(userId: UserId): F[List[PaymentWithMember]] = Sync[F].delay(List(PaymentWithMember(payment, member)))
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

  test("CREATE Payment") {
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
