package e2e

import com.itforelead.workout.domain.{Member, Validation}
import com.itforelead.workout.domain.custom.refinements.ValidationCode
import com.itforelead.workout.implicits.GenericTypeOps
import com.itforelead.workout.routes.deriveEntityDecoder
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto.exportDecoder
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.{MediaType, Status}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.multipart.{Multipart, Part}
import workout.utils.Generators._
import workout.utils.ClientSuite

import java.net.URL

object ArrivalRoutesSuite extends ClientSuite {

  test("Create Arrival E2E: Member Not Found") { implicit resources =>
    forall(createArrivalGen) { createArrival =>
      for {
        token <- loginReq.expectAs[JwtToken]
        res <- POST(createArrival, uri"/arrival")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.NotFound)
      } yield res
    }
  }

  test("Create Arrival E2E") { implicit resources =>
    val gen = for {
      ca <- createArrivalGen
      cm <- createMemberGen()
    } yield (ca, cm)
    forall(gen) { case (createArrival, createMember) =>
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
          .expectAs[Unit]
        getMember <- GET(uri"/member").putHeaders(makeAuth(token)).expectAs[List[Member]]
        memberId = getMember.find(_.phone == createMember.phone).get.id
        res <- POST(createArrival.copy(memberId = memberId), uri"/arrival")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Created)
      } yield res
    }
  }

  test("Get Arrival E2E") { implicit resources =>
    for {
      token <- loginReq.expectAs[JwtToken]
      result <- GET(uri"/arrival")
        .putHeaders(makeAuth(token))
        .expectHttpStatus(Status.Ok)
    } yield result
  }

}
