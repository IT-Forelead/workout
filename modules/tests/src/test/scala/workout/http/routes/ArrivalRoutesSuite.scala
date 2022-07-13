package workout.http.routes

import cats.effect.{IO, Sync}
import com.itforelead.workout.domain.{Arrival, Member}
import com.itforelead.workout.domain.Arrival.{ArrivalWithMember, ArrivalWithTotal, CreateArrival}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{ArrivalRoutes, deriveEntityEncoder}
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import workout.stub_services.ArrivalStub
import workout.utils.Generators._
import workout.utils.HttpSuite

object ArrivalRoutesSuite extends HttpSuite {
  private def arrivalMethod[F[_]: Sync: GenUUID](arrival: Arrival, member: Member): ArrivalStub[F] = new ArrivalStub[F] {
    override def create(userId: UserId, createArrival: CreateArrival): F[Arrival]  = Sync[F].delay(arrival)
    override def get(userId: UserId): F[List[ArrivalWithMember]] =
      Sync[F].delay(List(ArrivalWithMember(arrival, member)))
    override def getArrivalWithTotal(userId: UserId, page: Int): F[ArrivalWithTotal] =
      Sync[F].delay(ArrivalWithTotal(List(ArrivalWithMember(arrival, member)), 1))
  }

  test("GET Arrival") {
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

  test("CREATE Arrival") {
    val gen = for {
      u  <- userGen
      ca <- createArrivalGen
      a  <- arrivalGen
      m  <- memberGen
    } yield (u, ca, a, m)

    forall(gen) { case (user, createArrival, arrival, member) =>
      for {
        token <- authToken(user)
        req    = POST(createArrival, uri"/arrival").putHeaders(token)
        routes = new ArrivalRoutes[IO](arrivalMethod(arrival, member)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Created)
      } yield res
    }
  }
}
