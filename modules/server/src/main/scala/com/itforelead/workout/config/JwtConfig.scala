package com.itforelead.workout.config

import ciris.Secret
import com.itforelead.workout.types.{ JwtAccessTokenKeyConfig, PasswordSalt, TokenExpiration }

case class JwtConfig(
    tokenConfig: Secret[JwtAccessTokenKeyConfig],
    passwordSalt: Secret[PasswordSalt],
    tokenExpiration: TokenExpiration,
  )
