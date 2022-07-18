package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxOptionId}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentFilter, PaymentMemberId, PaymentWithMember, PaymentWithTotal}
import com.itforelead.workout.domain.custom.exception.{CreatePaymentDailyTypeError, MemberNotFound}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{Member, Payment, types}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{PaymentRoutes, deriveEntityEncoder}
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import weaver.Expectations
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
    override def getPaymentsWithTotal(userId: UserId, filter: PaymentFilter, page: Int): F[PaymentWithTotal] =
      Sync[F].delay(PaymentWithTotal(List(PaymentWithMember(payment, member)), 1))
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

  test("GET Payments pagination") {
    val gen = for {
      u <- userGen
      m <- memberGen
      p <- paymentGen
      f <- paymentFilterGen
    } yield (u, m, p, f)

    forall(gen) { case (user, member, payment, filter) =>
      for {
        token <- authToken(user)
        req = POST(filter, uri"/payment/1").putHeaders(token)
        routes = new PaymentRoutes[IO](paymentS(payment, member)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(PaymentWithTotal(List(PaymentWithMember(payment, member)), 1), Status.Ok)
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

  private def paymentServiceS[F[_]: Sync: GenUUID](
    payment: Payment,
    errorType: Option[String] = None
  ): PaymentsStub[F] =
    new PaymentsStub[F] {
      override def create(userId: UserId, createPayment: CreatePayment): F[Payment] = {
        errorType match {
          case None                                => Sync[F].delay(payment)
          case Some("createPaymentDailyTypeError") => CreatePaymentDailyTypeError.raiseError[F, Payment]
          case Some("memberNotFound")              => MemberNotFound.raiseError[F, Payment]
          case _ => Sync[F].raiseError(new Exception("Error occurred while creating payment. error type: Unknown"))
        }
      }
    }

  def postPaymentReq(errorType: Option[String] = None, shouldReturn: Status): PaymentRoutesSuite.F[Expectations] = {
    val gen = for {
      u  <- userGen
      cp <- createPaymentGen
      p  <- paymentGen
    } yield (u, cp, p)
    forall(gen) { case (user, createPay, payment) =>
      for {
        token <- authToken(user)
        req    = POST(createPay, uri"/payment").putHeaders(token)
        routes = new PaymentRoutes[IO](paymentServiceS(payment, errorType)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(shouldReturn)
      } yield res
    }
  }

  test("POST Payment") {
    postPaymentReq(shouldReturn = Status.Created)
  }

  test("POST Payment: Create Payment Daily Type Error") {
    postPaymentReq(errorType = "createPaymentDailyTypeError".some, shouldReturn = Status.MethodNotAllowed)
  }

  test("POST Payment: Member Not Found") {
    postPaymentReq(errorType = "memberNotFound".some, shouldReturn = Status.BadRequest)
  }

  test("POST Payment: Unknown Error") {
    postPaymentReq(errorType = "".some, shouldReturn = Status.BadRequest)
  }
}
