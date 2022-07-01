package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.Application.logger
import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.MemberRoutes
import org.http4s.Method.GET
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import workout.stub_services.MembersStub
import workout.utils.Generators._
import workout.utils.HttpSuite


object MemberRoutesSuite extends HttpSuite {

  private def members[F[_]: Sync: GenUUID](member: Member): MembersStub[F] = new MembersStub[F] {
    override def create(memberParam: CreateMember): F[Member] = Sync[F].delay(member)
    override def findByUserId(userId: UserId): F[List[Member]] = Sync[F].delay(List(member))
  }

  test("GET Member By User ID") {
    val gen = for {
      u <- userGen
      m <- memberGen
    } yield (u, m)

    forall(gen) { case (user, member) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/member").putHeaders(token)
        routes = new MemberRoutes[IO](members(member)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }
}
