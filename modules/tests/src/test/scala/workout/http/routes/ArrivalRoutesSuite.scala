package workout.http.routes

import cats.effect.{IO, Sync}
import com.itforelead.workout.domain.Arrival
import com.itforelead.workout.domain.Arrival.CreateArrival
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
  private def arrivalMethod[F[_]: Sync: GenUUID](arrival: Arrival): ArrivalStub[F] = new ArrivalStub[F] {
    override def create(userId: UserId, createArrival: CreateArrival): F[Arrival] = Sync[F].delay(arrival)
    override def get(userId: UserId): F[List[Arrival]]                            = Sync[F].delay(List(arrival))
  }

  test("GET Arrival") {
    val gen = for {
      u <- userGen
      a <- arrivalGen
    } yield (u, a)

    forall(gen) { case (user, arrival) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/arrival").putHeaders(token)
        routes = new ArrivalRoutes[IO](arrivalMethod(arrival)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("CREATE Arrival") {
    val gen = for {
      u  <- userGen
      ca <- createArrivalGen
      a  <- arrivalGen
    } yield (u, ca, a)

    forall(gen) { case (user, createArrival, arrival) =>
      for {
        token <- authToken(user)
        req    = POST(createArrival, uri"/arrival").putHeaders(token)
        routes = new ArrivalRoutes[IO](arrivalMethod(arrival)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Created)
      } yield res
    }
  }
}
