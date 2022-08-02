package com.itforelead.workout.services.mailer.data

import com.itforelead.workout.services.mailer.data.types.{ EmailAddress, Password }

case class Credentials(user: EmailAddress, password: Password)
