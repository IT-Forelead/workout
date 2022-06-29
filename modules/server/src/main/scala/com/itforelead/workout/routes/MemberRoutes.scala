//package com.itforelead.workout.routes
//
//import cats.MonadThrow
//import com.itforelead.workout.domain.Member.CreateMember
//import com.itforelead.workout.domain.{Member, User}
//import com.itforelead.workout.domain.custom.exception.PhoneInUse
//import io.circe.refined.refinedEncoder
//import org.http4s._
//import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
//import org.http4s.circe.JsonDecoder
//import org.http4s.dsl.Http4sDsl
//import org.http4s.server.{AuthMiddleware, Router}
//
//final class MemberRoutes[F[_]: JsonDecoder: MonadThrow] extends Http4sDsl[F] {
//
//  private[routes] val prefixPath = "/member"
//
//  private[this] val httpRoutes: AuthedRoutes[Member, F] = AuthedRoutes.of {
//
//  }
//
//  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
//    prefixPath -> authMiddleware(httpRoutes)
//  )
//
//}
