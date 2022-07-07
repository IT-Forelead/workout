package workout.http.routes

import cats.effect.{Async, IO, Sync}
import com.itforelead.workout.Application.logger
import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.custom.refinements.{FileKey, FilePath, Tel}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.MemberRoutes
import org.http4s.Method.GET
import org.http4s.client.dsl.io._
import org.http4s.{Status, Uri}
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

  test("GET Member By User ID") {
    val gen = for {
      u  <- userGen
      m  <- memberGen
      mt <- memberWithTotalGen
    } yield (u, m, mt)

    forall(gen) { case (user, member, memberWithTotal) =>
      for {
        token <- authToken(user)
        req    = GET(Uri.unsafeFromString(s"/member/1")).putHeaders(token)
        routes = new MemberRoutes[IO](members(member, memberWithTotal), s3Client()).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(memberWithTotal, Status.Ok)
      } yield res
    }
  }
}
