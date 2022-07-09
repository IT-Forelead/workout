package workout.http.routes

import cats.effect.{Async, IO, Sync}
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxOptionId}
import com.itforelead.workout.Application.logger
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeExpired, ValidationCodeIncorrect}
import com.itforelead.workout.domain.custom.refinements.{FileKey, FilePath, Tel}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{Member, User, Validation}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{MemberRoutes, deriveEntityEncoder}
import com.itforelead.workout.services.Members
import fs2.Pipe
import io.circe.generic.auto.exportEncoder
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{MediaType, Status}
import org.scalacheck.Gen
import workout.stub_services.{MembersStub, S3ClientMock}
import workout.utils.Generators._
import workout.utils.HttpSuite

import java.net.URL

object MemberRoutesSuite extends HttpSuite {

  private def s3Client[F[_]: Async](): S3ClientMock[F] = new S3ClientMock[F] {
    override def downloadObject(key: FilePath): fs2.Stream[F, Byte] = fs2.Stream.empty
    override def putObject(key: FilePath): Pipe[F, Byte, Unit]      = _ => fs2.Stream.empty
  }

  private def memberService[F[_]: Sync: GenUUID](
    member: Member,
    memberWithTotal: MemberWithTotal,
    isCorrect: Boolean,
    errorType: Option[String] = None
  ): MembersStub[F] =
    new MembersStub[F] {
      override def findMemberByPhone(phone: Tel): F[Option[Member]] = Sync[F].delay(Option(member))

      override def findByUserId(userId: UserId, page: Int): F[Member.MemberWithTotal] = Sync[F].delay(memberWithTotal)

      override def sendValidationCode(userId: UserId, phone: Tel): F[Unit] = Sync[F].unit

      override def validateAndCreate(userId: UserId, createMember: CreateMember, key: FileKey): F[Member] = {
        println(s"===========================")
        if (isCorrect) {
          println(s"===========================1")
          Sync[F].delay(member)
        }
        else if (errorType.contains("validationCodeExpired")) {
          println(s"===========================2")
          Sync[F].raiseError(new Exception("validationCodeExpired"))
        } else if (errorType.getOrElse("unknown") == "phoneInUse") {
          println(s"===========================3")
          PhoneInUse(createMember.phone).raiseError[F, Member]
        }
        else if (errorType.getOrElse("unknown") == "validationCodeIncorrect") {
          println(s"===========================4")
          ValidationCodeIncorrect(createMember.code).raiseError[F, Member]
        } else {
          println(s"===========================5")
          Sync[F].raiseError(new Exception("asdfasdfasdf"))
        }
      }
    }

  val gen: Gen[(User, Member, MemberWithTotal)] = for {
    u  <- userGen
    m  <- memberGen
    mt <- memberWithTotalGen
  } yield (u, m, mt)

  val genCreateMember: Gen[(User, Member, CreateMember, MemberWithTotal)] = for {
    u  <- userGen
    m  <- memberGen
    cm <- createMemberGen()
    mt <- memberWithTotalGen
  } yield (u, m, cm, mt)

  val genBadRequest: Gen[(User, CreateMember)] = for {
    u  <- userGen
    cm <- createMemberGen()
  } yield (u, cm)

  test("GET Member By User ID") {

    forall(gen) { case (user, member, memberWithTotal) =>
      for {
        token <- authToken(user)
        req = GET(uri"/member/1").putHeaders(token)
        routes = new MemberRoutes[IO](memberService(member, memberWithTotal, isCorrect = true), s3Client())
          .routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(memberWithTotal, Status.Ok)
      } yield res
    }
  }

  test("Send Validation Code") {

    forall(gen) { case (user, member, memberWithTotal) =>
      for {
        token <- authToken(user)
        req = POST(Validation(member.phone), uri"/member/sent-code").putHeaders(token)
        routes = new MemberRoutes[IO](memberService(member, memberWithTotal, isCorrect = true), s3Client())
          .routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("PUT Member") {
    forall(genCreateMember) { case (user, member, createMember, memberWithTotal) =>
      val fileUrl: URL = getClass.getResource("/photo_2022-06-28_16-27-20.jpg")
      for {
        token <- authToken(user)
        formData =
          Vector(
            Part.formData[F]("firstname", createMember.firstname.value.value),
            Part.formData[F]("lastname", createMember.lastname.value.value),
            Part.formData[F]("phone", createMember.phone.value),
            Part.formData[F]("birthday", createMember.birthday.toString),
            Part.formData[F]("code", createMember.code.value)
          )
        fileData = Vector(
          Part.fileData("filename", fileUrl, `Content-Type`(MediaType.image.`png`))
        )
        multipart = Multipart[F](formData ++ fileData)
        req       = PUT(multipart, uri"/member").withHeaders(multipart.headers).putHeaders(token)
        routes = new MemberRoutes[IO](memberService(member, memberWithTotal, isCorrect = true), s3Client())
          .routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Created)
      } yield res
    }
  }

//  test("PUT Member: File part isn't defined") {
//    forall(genCreateMember) { case (user, member, createMember, memberWithTotal) =>
//      val fileUrl: URL = getClass.getResource("")
//      for {
//        token <- authToken(user)
//        formData =
//          Vector(
//            Part.formData[F]("firstname", createMember.firstname.value.value),
//            Part.formData[F]("lastname", createMember.lastname.value.value),
//            Part.formData[F]("phone", createMember.phone.value),
//            Part.formData[F]("birthday", createMember.birthday.toString),
//            Part.formData[F]("code", createMember.code.value)
//          )
//        fileData = Vector(
//          Part.fileData("filename", fileUrl, `Content-Type`(MediaType.image.`png`))
//        )
//        multipart = Multipart[F](formData ++ fileData)
//        req       = PUT(multipart, uri"/member").withHeaders(multipart.headers).putHeaders(token)
//        routes    = new MemberRoutes[IO](memberService(member, memberWithTotal), s3Client()).routes(usersMiddleware)
//        res <- expectHttpStatus(routes, req)(Status.BadRequest)
//      } yield res
//    }
//  }

  test("PUT Member: Validation Code Incorrect") {
    forall(genCreateMember) { case (user, member, createMember, memberWithTotal) =>
      val fileUrl: URL = getClass.getResource("/photo_2022-06-28_16-27-20.jpg")
      for {
        token <- authToken(user)
        formData =
          Vector(
            Part.formData[F]("firstname", createMember.firstname.value.value),
            Part.formData[F]("lastname", createMember.lastname.value.value),
            Part.formData[F]("phone", createMember.phone.value),
            Part.formData[F]("birthday", createMember.birthday.toString),
            Part.formData[F]("code", createMember.code.value)
          )
        fileData = Vector(
          Part.fileData("filename", fileUrl, `Content-Type`(MediaType.image.`png`))
        )
        multipart = Multipart[F](formData ++ fileData)
        req       = PUT(multipart, uri"/member").withHeaders(multipart.headers).putHeaders(token)
        routes = new MemberRoutes[IO](
          memberService(member, memberWithTotal, isCorrect = false, "validationCodeIncorrect".some),
          s3Client()
        ).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.BadRequest)
      } yield res
    }
  }

  test("PUT Member: Validation Code Expired") {
    forall(genCreateMember) { case (user, member, createMember, memberWithTotal) =>
      val fileUrl: URL = getClass.getResource("/photo_2022-06-28_16-27-20.jpg")
      for {
        token <- authToken(user)
        formData =
          Vector(
            Part.formData[F]("firstname", createMember.firstname.value.value),
            Part.formData[F]("lastname", createMember.lastname.value.value),
            Part.formData[F]("phone", createMember.phone.value),
            Part.formData[F]("birthday", createMember.birthday.toString),
            Part.formData[F]("code", createMember.code.value)
          )
        fileData = Vector(
          Part.fileData("filename", fileUrl, `Content-Type`(MediaType.image.`png`))
        )
        multipart = Multipart[F](formData ++ fileData)
        req       = PUT(multipart, uri"/member").withHeaders(multipart.headers).putHeaders(token)
        routes = new MemberRoutes[IO](
          memberService(member, memberWithTotal, isCorrect = false, "validationCodeExpired".some),
          s3Client()
        ).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.BadRequest)
      } yield res
    }
  }

  test("PUT Member: Phone In Use") {
    forall(genCreateMember) { case (user, member, createMember, memberWithTotal) =>
      val fileUrl: URL = getClass.getResource("/photo_2022-06-28_16-27-20.jpg")
      for {
        token <- authToken(user)
        formData =
          Vector(
            Part.formData[F]("firstname", createMember.firstname.value.value),
            Part.formData[F]("lastname", createMember.lastname.value.value),
            Part.formData[F]("phone", createMember.phone.value),
            Part.formData[F]("birthday", createMember.birthday.toString),
            Part.formData[F]("code", createMember.code.value)
          )
        fileData = Vector(
          Part.fileData("filename", fileUrl, `Content-Type`(MediaType.image.`png`))
        )
        multipart = Multipart[F](formData ++ fileData)
        req       = PUT(multipart, uri"/member").withHeaders(multipart.headers).putHeaders(token)
        routes = new MemberRoutes[IO](
          memberService(member, memberWithTotal, isCorrect = false, "phoneInUse".some),
          s3Client()
        ).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.BadRequest)
      } yield res
    }
  }
}
