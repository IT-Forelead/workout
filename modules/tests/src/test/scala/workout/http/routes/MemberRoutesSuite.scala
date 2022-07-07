package workout.http.routes

import cats.effect.{Async, IO, Sync}
import com.itforelead.workout.Application.logger
import com.itforelead.workout.domain.{Member, User, Validation}
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.custom.refinements.{FileKey, FilePath, Tel}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.MemberRoutes
import io.circe.refined.refinedEncoder
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalacheck.Gen
import workout.stub_services.{MembersStub, S3ClientMock}
import workout.utils.Generators._
import workout.utils.HttpSuite

object MemberRoutesSuite extends HttpSuite {

  private def s3Client[F[_]: Async](): S3ClientMock[F] = new S3ClientMock[F] {
    override def downloadObject(key: FilePath): fs2.Stream[F, Byte] = fs2.Stream.empty
  }

  private def members[F[_]: Sync: GenUUID](member: Member, memberWithTotal: MemberWithTotal): MembersStub[F] =
    new MembersStub[F] {
      override def findMemberByPhone(phone: Tel): F[Option[Member]] = Sync[F].delay(Option(member))

      override def findByUserId(userId: UserId, page: Int): F[Member.MemberWithTotal] = Sync[F].delay(memberWithTotal)

      override def sendValidationCode(userId: UserId, phone: Tel): F[Unit] = Sync[F].unit

      override def validateAndCreate(userId: UserId, createMember: CreateMember, key: FileKey): F[Member] =
        Sync[F].delay(member)
    }

  val gen: Gen[(User, Member, MemberWithTotal)] = for {
    u  <- userGen
    m  <- memberGen
    mt <- memberWithTotalGen
  } yield (u, m, mt)

  test("GET Member By User ID") {

    forall(gen) { case (user, member, memberWithTotal) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/member/1").putHeaders(token)
        routes = new MemberRoutes[IO](members(member, memberWithTotal), s3Client()).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(memberWithTotal, Status.Ok)
      } yield res
    }
  }

  test("Send Validation Code") {

    forall(gen) { case (user, member, memberWithTotal) =>
      for {
        token <- authToken(user)
        req    = POST(Validation(member.phone), uri"/member/sent-code").putHeaders(token)
        routes = new MemberRoutes[IO](members(member, memberWithTotal), s3Client()).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("Send Validation Code") {

    forall(gen) { case (user, member, memberWithTotal) =>
      for {
        token <- authToken(user)
        req    = POST(Validation(member.phone), uri"/member/sent-code").putHeaders(token)
        routes = new MemberRoutes[IO](members(member, memberWithTotal), s3Client()).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }
}
