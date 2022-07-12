package e2e

import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.refinements.{Password, Tel}
import com.itforelead.workout.implicits.GenericTypeOps
import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{MediaType, Status}
import workout.utils.ClientSuite
import workout.utils.Generators.{createMemberGen, createPaymentGen}

import java.net.URL

object PaymentRoutesSuite extends ClientSuite {

  val fileUrl: URL = getClass.getResource("/photo_2022-06-28_16-27-20.jpg")
  val tel2: Tel           = Tel.unsafeFrom("+998911234567")
  val password2: Password = Password.unsafeFrom("Secret1!")

  test("Create") { implicit client =>
    val gen = for {
      p  <- createPaymentGen
      cm <- createMemberGen()
    } yield (p, cm)
    forall(gen) { case (payment, createMember) =>
      println(s"member paramas --------------------------------------------- $createMember")
      for {
        token <- loginReq(tel2.some, password2.some).expectAs[JwtToken]
//        _ <- POST(createMember.phone.value, uri"/member/sent-code")
//          .putHeaders(makeAuth(token))
//          .expectHttpStatus(Status.Ok)
        fileData  = Vector(Part.fileData("filename", fileUrl, `Content-Type`(MediaType.image.`png`)))
        multipart = Multipart[F](createMember.toFormData[F] ++ fileData)
//        _ <- PUT(multipart, uri"/member")
//          .withHeaders(multipart.headers)
//          .putHeaders(makeAuth(token))
//          .expectHttpStatus(Status.Created)
        members <- GET(uri"/member")
          .putHeaders(makeAuth(token))
          .expectAs[List[Member]]
        _ = println(s"__________________________________________________________________________ $members")
        memberId = members.head.id
        result <- POST(payment.copy(memberId = memberId), uri"/payment")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Created)
      } yield result
    }
  }

}
