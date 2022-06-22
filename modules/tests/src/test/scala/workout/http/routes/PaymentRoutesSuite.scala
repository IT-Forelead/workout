package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Payment
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
  def payments[F[_]: Sync: GenUUID](payment: Payment): PaymentsStub[F] = new PaymentsStub[F] {
    override def create(createPayment: CreatePayment): F[Payment] = Sync[F].delay(payment)
    override def payments: F[List[PaymentWithUser]]               = List.empty[PaymentWithUser].pure[F]
  }

  test("GET Payments") {
    val gen = for {
      p <- paymentGen
      u <- userGen
    } yield (p, u)

    forall(gen) { case (payment, user) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/payment").putHeaders(token)
        routes = new PaymentRoutes[IO](payments(payment)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("PUT Payment") {
    val gen = for {
      u  <- userGen
      cp <- createPaymentGen
      p  <- paymentGen
    } yield (u, cp, p)

    forall(gen) { case (user, createPayment, payment) =>
      for {
        token <- authToken(user)
        req    = POST(createPayment, uri"/payment").putHeaders(token)
        routes = new PaymentRoutes[IO](payments(payment)).routes(usersMiddleware)
        shouldReturn = if (user.role == ADMIN) Status.Created else Status.BadRequest
        res <- expectHttpStatus(routes, req)(shouldReturn)
      } yield res
    }
  }
}
