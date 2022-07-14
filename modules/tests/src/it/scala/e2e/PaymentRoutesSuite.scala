package e2e

import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain.Payment.CreatePayment
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.refinements.ValidationCode
import com.itforelead.workout.domain.types.MemberId
import com.itforelead.workout.implicits.GenericTypeOps
import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{MediaType, Status}
import weaver.Expectations
import workout.utils.ClientSuite
import workout.utils.Generators.createMemberGen

import java.net.URL
import java.util.UUID

object PaymentRoutesSuite extends ClientSuite {

  val fileUrl: Option[URL] = Option(getClass.getResource("/photo_2022-06-28_16-27-20.jpg"))

  def createPaymentRequest(
    shouldReturn: Status,
    memberId: Option[MemberId] = None,
    paymentType: PaymentType
  )(implicit resources: Res): PaymentRoutesSuite.F[Expectations] = {
    forall(createMemberGen()) { createMember =>
      for {
        token <- loginReq.expectAs[JwtToken]
        _     <- POST(Validation(createMember.phone), uri"/member/sent-code").putHeaders(makeAuth(token)).expectAs[Unit]
        fileData = fileUrl.map { url =>
          Part.fileData("filename", url, `Content-Type`(MediaType.image.`jpeg`))
        }.toVector
        code <- resources.redis.get(createMember.phone.value)
        member    = createMember.copy(code = ValidationCode.unsafeFrom(code.get))
        multipart = Multipart[F](member.toFormData[F] ++ fileData)
        _ <- PUT(multipart, uri"/member")
          .withHeaders(multipart.headers)
          .putHeaders(makeAuth(token))
          .expectAs[Unit]
        getMember <- GET(uri"/member").putHeaders(makeAuth(token)).expectAs[List[Member]]
        memberIdDB = getMember.find(_.phone == createMember.phone).get.id
        result <- POST(CreatePayment(memberId.getOrElse(memberIdDB), paymentType), uri"/payment")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(shouldReturn)
      } yield result
    }
  }

  test("Create Payment") { implicit client =>
    createPaymentRequest(Status.Created, paymentType = PaymentType.DAILY)
  }

  test("Create Payment: Member Not Found") { implicit client =>
    createPaymentRequest(
      Status.BadRequest,
      memberId = MemberId(UUID.randomUUID()).some,
      paymentType = PaymentType.DAILY
    )
  }

  test("Get Payments By UserId") { implicit client =>
    for {
      token  <- loginReq.expectAs[JwtToken]
      result <- GET(uri"/payment").putHeaders(makeAuth(token)).expectHttpStatus(Status.Ok)
    } yield result

  }

}
