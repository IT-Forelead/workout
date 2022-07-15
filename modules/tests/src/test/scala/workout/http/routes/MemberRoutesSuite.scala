package workout.http.routes

import cats.Show.catsStdShowForTuple2
import cats.effect.{IO, Sync}
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxOptionId}
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.custom.exception.{MultipartDecodeError, PhoneInUse, ValidationCodeExpired, ValidationCodeIncorrect}
import com.itforelead.workout.domain.custom.refinements.{FileKey, FilePath, Tel}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{Member, User, Validation, encCreateMemberAsObject}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.implicits.GenericTypeOps
import com.itforelead.workout.routes.{MemberRoutes, deriveEntityEncoder}
import eu.timepit.refined.cats.refTypeShow
import fs2.{Pipe, Stream}
import io.circe.generic.auto.exportEncoder
import org.http4s._
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.multipart.{Multipart, Part}
import org.scalacheck.Gen
import weaver.Expectations
import workout.stub_services.{MembersStub, S3ClientMock}
import workout.utils.Generators._
import workout.utils.HttpSuite

import java.net.URL

object MemberRoutesSuite extends HttpSuite {

  private val s3Client: S3ClientMock[IO] = new S3ClientMock[IO] {
    override def downloadObject(key: FilePath): fs2.Stream[IO, Byte] = fs2.Stream.empty
    override def putObject(key: FilePath): Pipe[IO, Byte, Unit]      = (s: Stream[IO, Byte]) => s.as(())
  }

  private def memberService[F[_]: Sync: GenUUID](
    member: Member
  ): MembersStub[F] =
    new MembersStub[F] {

      override def get(userId: UserId): F[List[Member]] = Sync[F].delay(List(member))

      override def findMemberByPhone(phone: Tel): F[Option[Member]] = Sync[F].delay(Option(member))

      override def findByUserId(userId: UserId, page: Int): F[Member.MemberWithTotal] =
        Sync[F].delay(MemberWithTotal(List(member), 1))

      override def sendValidationCode(userId: UserId, phone: Tel): F[Unit] = Sync[F].unit
    }

  val gen: Gen[(User, Member)] = for {
    u <- userGen
    m <- memberGen
  } yield u -> m

  test("GET Members - [SUCCESS]") {
    forall(gen) { case user -> member =>
      for {
        token <- authToken(user)
        req = GET(uri"/member").putHeaders(token)
        routes = new MemberRoutes[IO](memberService(member), s3Client)
          .routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("GET Member By User ID - [SUCCESS]") {
    forall(gen) { case user -> member =>
      for {
        token <- authToken(user)
        req = GET(uri"/member/1").putHeaders(token)
        routes = new MemberRoutes[IO](memberService(member), s3Client)
          .routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(MemberWithTotal(List(member), 1), Status.Ok)
      } yield res
    }
  }

  test("Send Validation Code - [SUCCESS]") {
    forall(gen) { case user -> member =>
      for {
        token <- authToken(user)
        req = POST(Validation(member.phone), uri"/member/sent-code").putHeaders(token)
        routes = new MemberRoutes[IO](memberService(member), s3Client)
          .routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  private def memberServiceS[F[_]: Sync: GenUUID](
    member: Member,
    errorType: Option[String] = None
  ): MembersStub[F] =
    new MembersStub[F] {
      override def validateAndCreate(userId: UserId, createMember: CreateMember, key: FileKey): F[Member] = {
        errorType match {
          case None                            => Sync[F].delay(member)
          case Some("validationCodeExpired")   => ValidationCodeExpired(createMember.phone).raiseError[F, Member]
          case Some("phoneInUse")              => PhoneInUse(createMember.phone).raiseError[F, Member]
          case Some("validationCodeIncorrect") => ValidationCodeIncorrect(createMember.code).raiseError[F, Member]
          case Some("multipartDecodeError")    => MultipartDecodeError("Multipart Decode Error").raiseError[F, Member]
          case _ => Sync[F].raiseError(new Exception("Error occurred creating member. error type: Unknown"))
        }
      }
    }

  def putMemberRequest(
    shouldReturn: Status,
    errorType: Option[String] = None,
    fileUrl: Option[URL] = None,
    multipartDecError: Boolean = false
  ): MemberRoutesSuite.F[Expectations] = {
    val gen = for {
      u  <- userGen
      m  <- memberGen
      cm <- createMemberGen()
    } yield (u, m, cm)

    forall(gen) { case (user, member, createMember) =>
      for {
        token <- authToken(user)
        fileData = fileUrl.map { url =>
          Part.fileData("filename", url, `Content-Type`(MediaType.image.`png`))
        }.toVector
        multipart =
          if (multipartDecError) Multipart[F](fileData) else Multipart[F](createMember.toFormData[F] ++ fileData)
        req = PUT(multipart, uri"/member").withHeaders(multipart.headers).putHeaders(token)
        routes = new MemberRoutes[IO](memberServiceS(member, errorType), s3Client)
          .routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(shouldReturn)
      } yield res
    }
  }
  val fileUrl: URL = getClass.getResource("/photo_2022-06-28_16-27-20.jpg")

  test("PUT Member - [SUCCESS]") {
    putMemberRequest(Status.Created, fileUrl = fileUrl.some)
  }
  test("PUT Member: Validation Code Incorrect - [FAIL]") {
    putMemberRequest(Status.NotAcceptable, "validationCodeIncorrect".some, fileUrl = fileUrl.some)
  }
  test("PUT Member: File part isn't defined - [FAIL]") {
    putMemberRequest(Status.BadRequest)
  }
  test("PUT Member: Validation Code Expired - [FAIL]") {
    putMemberRequest(Status.NotAcceptable, "validationCodeExpired".some, fileUrl = fileUrl.some)
  }
  test("PUT Member: Phone In Use - [FAIL]") {
    putMemberRequest(Status.NotAcceptable, "phoneInUse".some, fileUrl = fileUrl.some)
  }
  test("PUT Member: Multipart Decode Error - [FAIL]") {
    putMemberRequest(Status.BadRequest, "multipartDecodeError".some, fileUrl = fileUrl.some, multipartDecError = true)
  }
  test("PUT Member: Unknown Error - [FAIL]") {
    putMemberRequest(Status.BadRequest, "".some, fileUrl = fileUrl.some)
  }

  test("Image Stream") {
    val gen: Gen[(Member, FilePath)] = for {
      m <- memberGen
      f <- filePathGen
    } yield (m, f)

    forall(gen) { case (member, filePath) =>
      val routes = new MemberRoutes[IO](memberServiceS(member), s3Client).routes(usersMiddleware)
      val req    = GET(Uri.unsafeFromString(s"/member/image/$filePath"))
      expectHttpStatus(routes, req)(Status.Ok)
    }
  }
}
