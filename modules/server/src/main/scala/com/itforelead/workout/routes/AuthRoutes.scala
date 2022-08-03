package com.itforelead.workout.routes

import cats.syntax.all._
import cats.{ Monad, MonadThrow }
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.CreateClient
import com.itforelead.workout.services.Auth
import com.itforelead.workout.implicits.http4SyntaxReqOps
import dev.profunktor.auth.AuthHeaders
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import com.itforelead.workout.domain
import com.itforelead.workout.domain.custom.exception._
import com.itforelead.workout.domain.tokenCodec
import org.typelevel.log4cats.Logger

final case class AuthRoutes[F[_]: Monad: JsonDecoder: MonadThrow](
    auth: Auth[F]
  )(implicit
    logger: Logger[F]
  ) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val publicRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        req.decodeR[domain.Credentials] { credentials =>
          auth
            .login(credentials)
            .flatMap(Ok(_))
            .recoverWith {
              case UserNotFound(_) | InvalidPassword(_) =>
                Forbidden("Telefon raqam yoki parol noto'g'ri.")
              case UserNotActivated =>
                NotAcceptable("Sizning profilingiz aktivlash jarayonida. Iltimos biroz kuting!")
            }
        }

      case req @ POST -> Root / "user" =>
        req.decodeR[CreateClient] { createClient =>
          auth
            .newUser(createClient)
            .flatMap(Created(_))
            .recoverWith {
              case codeExpiredError: ValidationCodeExpired =>
                logger.error(s"Validation code expired. Error: ${codeExpiredError.phone.value}") >>
                  NotAcceptable(
                    "Tasdiqlash kodi muddati tugagan. Iltimos, yana bir bor urinib ko'ring."
                  )
              case phoneInUseError: PhoneInUse =>
                logger.error(s"Phone is already in use. Error: ${phoneInUseError.phone.value}") >>
                  NotAcceptable(
                    "Telefon allaqachon ishlatilmoqda. Boshqa telefon raqami bilan qayta urinib koʻring"
                  )
              case valCodeError: ValidationCodeIncorrect =>
                logger.error(s"Validation code is wrong. Error: ${valCodeError.code.value}") >>
                  NotAcceptable("Tasdiqlash kodi noto'g'ri. Iltimos, yana bir bor urinib ko'ring")
              case error =>
                logger.error(error)("Error occurred creating user!") >>
                  BadRequest(
                    "Foydalanuvchini yaratishda xatolik yuz berdi. Iltimos, yana bir bor urinib ko'ring!"
                  )
            }
        }

    }

  private[this] val privateRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ GET -> Root / "logout" as user =>
      AuthHeaders
        .getBearerToken(ar.req)
        .traverse_(auth.logout(_, user.phone)) *> NoContent()

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> (publicRoutes <+> authMiddleware(privateRoutes))
  )
}
