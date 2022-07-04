package workout.http.routes

import cats.effect.{IO, Sync}
import com.itforelead.workout.Application.logger
import com.itforelead.workout.domain.{DeliveryStatus, Member, Message, types}
import com.itforelead.workout.domain.Message.{CreateMessage, MessageWithMember}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{MessageRoutes, deriveEntityEncoder}
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import workout.stub_services.MessagesStub
import workout.utils.Generators._
import workout.utils.HttpSuite

object MessageRoutesSuite extends HttpSuite {
  private def messages[F[_]: Sync: GenUUID](message: Message, member: Member): MessagesStub[F] = new MessagesStub[F] {
//    override def create(msg: CreateMessage): F[Message] = Sync[F].delay(message)
    override def get(userId: UserId): F[List[MessageWithMember]] = Sync[F].delay(List(MessageWithMember(message, member)))
//    override def changeStatus(id: types.MessageId, status: DeliveryStatus): F[Message] = Sync[F].delay(message)
  }

  test("GET Messages") {
    val gen = for {
      u  <- userGen
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

//  test("CREATE Message") {
//    val gen = for {
//      u  <- userGen
//      cm <- createMessageGen
//      ms  <- messageGen
//      m  <- memberGen
//    } yield (u, cm, ms, m)
//
//    forall(gen) { case (user, createMessage, message, member) =>
//      for {
//        token <- authToken(user)
//        req    = POST(createMessage, uri"/message").putHeaders(token)
//        routes = new MessageRoutes[IO](messages(message, member)).routes(usersMiddleware)
//        res <- expectHttpStatus(routes, req)(Status.Created)
//      } yield res
//    }
//  }
}