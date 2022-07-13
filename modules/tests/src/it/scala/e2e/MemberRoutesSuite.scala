package e2e
import cats.effect.IO
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.refinements.ValidationCode
import com.itforelead.workout.implicits.GenericTypeOps
import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.{POST, PUT}
import org.http4s.{MediaType, Status}
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.http4s.multipart.{Multipart, Part}
import weaver.Expectations
import workout.utils.ClientSuite
import workout.utils.Generators.{createMemberGen, validationGen}

import java.net.URL

object MemberRoutesSuite extends ClientSuite {

  test("Send Code E2E") { implicit resources =>
    forall(validationGen) { validation =>
      for {
        token <- loginReq.expectAs[JwtToken]
        result <- POST(validation, uri"/member/sent-code")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Ok)
      } yield result
    }
  }

  val fileUrl: Option[URL] = Option(getClass.getResource("/photo_2022-06-28_16-27-20.jpg"))

  def putMemberRequest(
    shouldReturn: Status,
    validationCodeIncorrect: Boolean = false,
    validationCodeExpired: Boolean = false,
    phoneInUse: Boolean = false,
    fileUrl: Option[URL] = fileUrl
  )(implicit resources: Resources[IO]): MemberRoutesSuite.F[Expectations] = {
    forall(createMemberGen()) { createMember =>
      for {
        token <- loginReq.expectAs[JwtToken]
        _ <-
          if (validationCodeExpired)
            IO.unit
          else POST(Validation(createMember.phone), uri"/member/sent-code").putHeaders(makeAuth(token)).expectAs[Unit]

        fileData = fileUrl.map { url =>
          Part.fileData("filename", url, `Content-Type`(MediaType.image.`jpeg`))
        }.toVector

        code <- if (validationCodeExpired) IO.pure(None) else resources.redis.get(createMember.phone.value)
        member =
          if (validationCodeIncorrect) createMember
          else {
            code match {
              case Some(value) => createMember.copy(code = ValidationCode.unsafeFrom(value))
              case None        => createMember
            }

          }
        multipart = Multipart[F](member.toFormData[F] ++ fileData)

        _ <-
          if (phoneInUse)
            PUT(multipart, uri"/member")
              .withHeaders(multipart.headers)
              .putHeaders(makeAuth(token))
              .expectAs[Unit]
          else IO.unit

        result <- PUT(multipart, uri"/member")
          .withHeaders(multipart.headers)
          .putHeaders(makeAuth(token))
          .expectHttpStatus(shouldReturn)
      } yield result
    }
  }

  test("Validate & Create") { implicit resources =>
    putMemberRequest(Status.Created)
  }

  test("Validate & Create: File Part Is Not Defined") { implicit resources =>
    putMemberRequest(Status.BadRequest, fileUrl = None)
  }

  test("Validate & Create: Validation Code Incorrect") { implicit resources =>
    putMemberRequest(Status.NotAcceptable, validationCodeIncorrect = true)
  }

  test("Validate & Create: Validation Code Expired") { implicit resources =>
    putMemberRequest(Status.NotAcceptable, validationCodeExpired = true)
  }

  test("Validate & Create: Phone In Use") { implicit resources =>
    putMemberRequest(Status.NotAcceptable, phoneInUse = true)
  }
}
