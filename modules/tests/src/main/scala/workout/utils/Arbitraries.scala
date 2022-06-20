package workout.utils

import com.itforelead.workout.domain.custom.refinements.{EmailAddress, FileName, Password, Tel}
import com.itforelead.workout.domain.{Gender, Role}
import org.http4s.MediaType
import org.scalacheck.Gen._
import org.scalacheck.{Arbitrary, Gen}
import Generators.{nonEmptyStringGen, numberGen}

import java.time.LocalDateTime

object Arbitraries {

  implicit lazy val arbGender: Arbitrary[Gender] = Arbitrary(oneOf(Gender.genders))
  implicit lazy val arbRole: Arbitrary[Role]     = Arbitrary(oneOf(Role.roles))
  implicit lazy val arbLocalDateTime: Arbitrary[LocalDateTime] = Arbitrary(
    for {
      year   <- Gen.choose(1800, 2100)
      month  <- Gen.choose(1, 12)
      day    <- Gen.choose(1, 28)
      hour   <- Gen.choose(0, 23)
      minute <- Gen.choose(0, 59)
    } yield LocalDateTime.of(year, month, day, hour, minute)
  )
  implicit lazy val arbEmail: Arbitrary[EmailAddress] = Arbitrary(
    for {
      s0 <- nonEmptyStringGen(4, 8)
      s1 <- nonEmptyStringGen(3, 5)
      s2 <- nonEmptyStringGen(2, 3)
    } yield EmailAddress.unsafeFrom(s"$s0@$s1.$s2")
  )

  implicit lazy val arbPassword: Arbitrary[Password] = Arbitrary(
    for {
      s0 <- alphaUpperChar
      s1 <- nonEmptyStringGen(5, 8)
      s2 <- numChar
      s3 <- oneOf("!@#$%^&*")
    } yield Password.unsafeFrom(s"$s0$s1$s2$s3")
  )

  implicit lazy val arbFileName: Arbitrary[FileName] = Arbitrary(
    for {
      s0 <- nonEmptyStringGen(5, 30)
      s1 <- oneOf(MediaType.allMediaTypes.flatMap(_.fileExtensions))
    } yield FileName.unsafeFrom(s"$s0.$s1")
  )

  implicit lazy val arbTel: Arbitrary[Tel] = Arbitrary(
    for {
      s0 <- numberGen(12)
    } yield Tel.unsafeFrom(s"+$s0")
  )
}
