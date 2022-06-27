package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.domain.{Payment, User}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithUser}
import com.itforelead.workout.domain.Role.ADMIN
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
  private def paymentS[F[_]: Sync: GenUUID](payment: Payment, user: User): PaymentsStub[F] = new PaymentsStub[F] {
    override def create(createPayment: CreatePayment): F[Payment] = Sync[F].delay(payment)
    override def payments: F[List[PaymentWithUser]]               = Sync[F].delay(List(PaymentWithUser(payment, user)))
  }

  test("GET Payments") {
    val gen = for {
      u <- userGen
      m <- userGen
      p <- paymentGen
    } yield (u, m, p)

    forall(gen) { case (user, member, payment) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/payment").putHeaders(token)
        routes = new PaymentRoutes[IO](paymentS(payment, member)).routes(usersMiddleware)
        res <- if (user.role == ADMIN) expectHttpStatus(routes, req)(Status.Ok) else expectNotFound(routes, req)
      } yield res
    }
  }

  test("CREATE Payment") {
    val gen = for {
      u  <- userGen
      m  <- userGen
      cp <- createPaymentGen
      p  <- paymentGen
    } yield (u, m, cp, p)

    forall(gen) { case (user, member, createPay, payment) =>
      for {
        token <- authToken(user)
        req    = POST(createPay, uri"/payment").putHeaders(token)
        routes = new PaymentRoutes[IO](paymentS(payment, member)).routes(usersMiddleware)
        res <- if (user.role == ADMIN) expectHttpStatus(routes, req)(Status.Created) else expectNotFound(routes, req)
      } yield res
    }
  }
}
