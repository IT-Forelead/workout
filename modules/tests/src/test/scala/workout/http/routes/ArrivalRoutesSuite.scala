package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits.{catsSyntaxApplicativeErrorId, catsSyntaxOptionId}
import com.itforelead.workout.domain.{Arrival, Member, User}
import com.itforelead.workout.domain.Arrival.{ArrivalWithMember, ArrivalWithTotal, CreateArrival}
import com.itforelead.workout.domain.custom.exception.MemberNotFound
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{ArrivalRoutes, deriveEntityEncoder}
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalacheck.Gen
import weaver.Expectations
import workout.stub_services.ArrivalStub
import workout.utils.Generators._
import workout.utils.HttpSuite

object ArrivalRoutesSuite extends HttpSuite {
  private def arrivalMethod[F[_]: Sync: GenUUID](
    arrival: Arrival,
    member: Member,
    errorType: Option[String] = None
  ): ArrivalStub[F] =
    new ArrivalStub[F] {
      override def create(userId: UserId, createArrival: CreateArrival): F[Arrival] =
        errorType match {
          case None             => Sync[F].delay(arrival)
          case Some("memberNotFound") => MemberNotFound.raiseError[F, Arrival]
          case _ => Sync[F].raiseError(new Exception("Error occurred creating arrival event. error type: Unknown"))
        }
      override def get(userId: UserId): F[List[ArrivalWithMember]] =
        Sync[F].delay(List(ArrivalWithMember(arrival, member)))
    override def getArrivalWithTotal(userId: UserId, page: Int): F[ArrivalWithTotal] =
      Sync[F].delay(ArrivalWithTotal(List(ArrivalWithMember(arrival, member)), 1))
  }

  test("GET Arrival - [SUCCESS]") {
    val gen = for {
      u <- userGen
      a <- arrivalGen
      m <- memberGen
    } yield (u, a, m)

    forall(gen) { case (user, arrival, member) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/arrival").putHeaders(token)
        routes = new ArrivalRoutes[IO](arrivalMethod(arrival, member)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("GET Arrival pagenation") {
    val gen = for {
      u <- userGen
      a <- arrivalGen
      m <- memberGen
    } yield (u, a, m)

    forall(gen) { case (user, arrival, member) =>
      for {
        token <- authToken(user)
        req = GET(uri"/arrival/1").putHeaders(token)
        routes = new ArrivalRoutes[IO](arrivalMethod(arrival, member)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(ArrivalWithTotal(List(ArrivalWithMember(arrival, member)), 1), Status.Ok)
      } yield res
    }
  }

  def createMemberReq(shouldReturn: Status, errorType: Option[String] = None): ArrivalRoutesSuite.F[Expectations] = {

    val createGen: Gen[(User, CreateArrival, Arrival, Member)] = for {
      u  <- userGen
      ca <- createArrivalGen
      a  <- arrivalGen
      m  <- memberGen
    } yield (u, ca, a, m)

    forall(createGen) { case (user, createArrival, arrival, member) =>
      for {
        token <- authToken(user)
        req    = POST(createArrival, uri"/arrival").putHeaders(token)
        routes = new ArrivalRoutes[IO](arrivalMethod(arrival, member, errorType)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(shouldReturn)
      } yield res
    }
  }

  test("CREATE Arrival - [SUCCESS]") {
    createMemberReq(Status.Created)
  }
  test("CREATE Arrival: Member Not Found - [FAIL]") {
    createMemberReq(Status.NotFound, "memberNotFound".some)
  }
}
