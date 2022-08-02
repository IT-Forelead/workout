package com.itforelead.workout.routes

import cats.effect.Async
import cats.implicits._
import com.itforelead.workout.domain.Message.MessagesFilter
import com.itforelead.workout.domain.{ User, Validation }
import com.itforelead.workout.services.Messages
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }

final class MessageRoutes[F[_]: Async](messages: Messages[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/message"

  private[this] val privateRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {

    case GET -> Root as user =>
      messages.get(user.id).flatMap(Ok(_))

    case ar @ POST -> Root / IntVar(page) as user =>
      ar.req.decodeR[MessagesFilter] { filter =>
        messages.getMessagesWithTotal(user.id, filter, page).flatMap(Ok(_))
      }

    case aR @ POST -> Root / "sent-code" as user =>
      aR.req.decodeR[Validation] { validationPhone =>
        messages.sendValidationCode(user.id.some, validationPhone.phone).flatMap(Ok(_))
      }

  }

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "public" / "sent-code" =>
      req.decodeR[Validation] { validationPhone =>
        messages.sendValidationCode(phone = validationPhone.phone).flatMap(Ok(_))
      }

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> (publicRoutes <+> authMiddleware(privateRoutes))
  )
}
