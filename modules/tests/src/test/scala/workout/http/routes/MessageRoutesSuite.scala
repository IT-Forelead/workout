package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain.Message.{MessageWithMember, MessageWithTotal, MessagesFilter}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{Member, Message, User, Validation}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{MessageRoutes, deriveEntityEncoder}
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalacheck.Gen
import workout.stub_services.MessagesStub
import workout.utils.Generators._
import workout.utils.HttpSuite

object MessageRoutesSuite extends HttpSuite {
  private def messages[F[_]: Sync: GenUUID](message: Message, member: Member): MessagesStub[F] = new MessagesStub[F] {
    override def get(userId: UserId): F[List[MessageWithMember]] =
      Sync[F].delay(List(MessageWithMember(message, member.some)))
    override def getMessagesWithTotal(userId: UserId, filter: MessagesFilter, page: Int): F[MessageWithTotal] =
      Sync[F].delay(MessageWithTotal(List(MessageWithMember(message, member.some)), 1))

    override def sendValidationCode(
      userId: UserId,
      tel: Tel
    ): F[Unit] = Sync[F].unit
  }

  test("GET Messages") {
    val gen = for {
      u  <- userGen()
      ms <- messageGen
      m  <- memberGen
    } yield (u, ms, m)

    forall(gen) { case (user, message, member) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/message").putHeaders(token)
        routes = new MessageRoutes[IO](messages(message, member)).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

  test("GET Messages Pagination") {
    val gen = for {
      u  <- userGen()
      ms <- messageGen
      m  <- memberGen
      f <- messageFilterGen
    } yield (u, ms, m, f)

    forall(gen) { case (user, message, member, filter) =>
      for {
        token <- authToken(user)
        req    = POST(filter, uri"/message/1").putHeaders(token)
        routes = new MessageRoutes[IO](messages(message, member)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(
          MessageWithTotal(List(MessageWithMember(message, member.some)), 1),
          Status.Ok
        )
      } yield res
    }
  }

  test("Send Validation Code - [SUCCESS]") {
    val gen: Gen[(User, Member, Message)] = for {
      u <- userGen()
      m <- memberGen
      ms <- messageGen
    } yield (u, m, ms)
    forall(gen) { case (user, member, message) =>
      for {
        token <- authToken(user)
        req = POST(Validation(member.phone), uri"/message/sent-code").putHeaders(token)
        routes = new MessageRoutes[IO](messages(message, member))
          .routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(Status.Ok)
      } yield res
    }
  }

}
