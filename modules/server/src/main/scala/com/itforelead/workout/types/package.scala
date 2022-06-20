package com.itforelead.workout

import derevo.cats.show
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import com.itforelead.workout.utils.ciris.configDecoder
import io.circe.refined._

import scala.concurrent.duration.FiniteDuration
import ciris.refined._
import com.itforelead.workout.utils.ciris.configDecoder
import eu.timepit.refined.cats._

package object types {

  @derive(configDecoder, show)
  @newtype case class AdminUserTokenConfig(secret: NonEmptyString)

  @derive(configDecoder, show)
  @newtype case class JwtSecretKeyConfig(secret: NonEmptyString)

  @derive(configDecoder, show)
  @newtype case class JwtAccessTokenKeyConfig(secret: NonEmptyString)

//  @derive(configDecoder, show)
//  @newtype case class JwtClaimConfig(secret: NonEmptyString)

  @derive(configDecoder, show)
  @newtype case class PasswordSalt(secret: NonEmptyString)

  @newtype case class TokenExpiration(value: FiniteDuration)

}
