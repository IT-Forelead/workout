package e2e
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

  test("Validate & Create") { implicit resources =>
    forall(createMemberGen()) { createMember =>
      for {
        token <- loginReq.expectAs[JwtToken]
        _     <- POST(Validation(createMember.phone), uri"/member/sent-code").putHeaders(makeAuth(token)).expectAs[Unit]
        fileUrl: Option[URL] = Option(getClass.getResource("/photo_2022-06-28_16-27-20.jpg"))
        fileData = fileUrl.map { url =>
          Part.fileData("filename", url, `Content-Type`(MediaType.image.`jpeg`))
        }.toVector
        code <- resources.redis.get(createMember.phone.value)
        member    = createMember.copy(code = ValidationCode.unsafeFrom(code.get))
        multipart = Multipart[F](member.toFormData[F] ++ fileData)
        result <- PUT(multipart, uri"/member")
          .withHeaders(multipart.headers)
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Created)
      } yield result
    }
  }

  test("Validate & Create: File Part Is Not Defined") { implicit resources =>
    forall(createMemberGen()) { createMember =>
      for {
        token <- loginReq.expectAs[JwtToken]
        _     <- POST(Validation(createMember.phone), uri"/member/sent-code").putHeaders(makeAuth(token)).expectAs[Unit]
        fileUrl: Option[URL] = Option(getClass.getResource(""))
        fileData = fileUrl.map { url =>
          Part.fileData("filename", url, `Content-Type`(MediaType.image.`jpeg`))
        }.toVector
        code <- resources.redis.get(createMember.phone.value)
        member    = createMember.copy(code = ValidationCode.unsafeFrom(code.get))
        multipart = Multipart[F](member.toFormData[F] ++ fileData)
        result <- PUT(multipart, uri"/member")
          .withHeaders(multipart.headers)
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.BadRequest)
      } yield result
    }
  }

  test("Validate & Create: Validation Code Incorrect") { implicit resources =>
    forall(createMemberGen()) { createMember =>
      for {
        token <- loginReq.expectAs[JwtToken]
        _     <- POST(Validation(createMember.phone), uri"/member/sent-code").putHeaders(makeAuth(token)).expectAs[Unit]
        fileUrl: Option[URL] = Option(getClass.getResource("/photo_2022-06-28_16-27-20.jpg"))
        fileData = fileUrl.map { url =>
          Part.fileData("filename", url, `Content-Type`(MediaType.image.`jpeg`))
        }.toVector
        multipart = Multipart[F](createMember.toFormData[F] ++ fileData)
        result <- PUT(multipart, uri"/member")
          .withHeaders(multipart.headers)
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.NotAcceptable)
      } yield result
    }
  }

  test("Validate & Create: Validation Code Expired") { implicit resources =>
    forall(createMemberGen()) { createMember =>
      for {
        token <- loginReq.expectAs[JwtToken]
        fileUrl: Option[URL] = Option(getClass.getResource("/photo_2022-06-28_16-27-20.jpg"))
        fileData = fileUrl.map { url =>
          Part.fileData("filename", url, `Content-Type`(MediaType.image.`jpeg`))
        }.toVector
        multipart = Multipart[F](createMember.toFormData[F] ++ fileData)
        result <- PUT(multipart, uri"/member")
          .withHeaders(multipart.headers)
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.NotAcceptable)
      } yield result
    }
  }

  test("Validate & Create: Phone In Use") { implicit resources =>
    val gen = for {
      m1 <- createMemberGen()
      m2 <- createMemberGen()
    } yield (m1, m2)
    forall(gen) { case (createMember1, createMember2) =>
      for {
        token <- loginReq.expectAs[JwtToken]
        _     <- POST(Validation(createMember1.phone), uri"/member/sent-code").putHeaders(makeAuth(token)).expectAs[Unit]
        fileUrl: Option[URL] = Option(getClass.getResource("/photo_2022-06-28_16-27-20.jpg"))
        fileData = fileUrl.map { url =>
          Part.fileData("filename", url, `Content-Type`(MediaType.image.`jpeg`))
        }.toVector
        code1 <- resources.redis.get(createMember1.phone.value)
        member1    = createMember1.copy(code = ValidationCode.unsafeFrom(code1.get))
        multipart1 = Multipart[F](member1.toFormData[F] ++ fileData)
        _ <- PUT(multipart1, uri"/member")
          .withHeaders(multipart1.headers)
          .putHeaders(makeAuth(token))
          .expectAs[Unit]

        member2    = createMember2.copy(code = ValidationCode.unsafeFrom(code1.get), phone = createMember1.phone)
        multipart2 = Multipart[F](member2.toFormData[F] ++ fileData)
        result <- PUT(multipart2, uri"/member")
          .withHeaders(multipart2.headers)
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.NotAcceptable)
      } yield result
    }
  }
}
