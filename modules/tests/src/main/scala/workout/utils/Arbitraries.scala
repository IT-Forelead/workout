package workout.utils

import com.itforelead.workout.domain.custom.refinements._
import com.itforelead.workout.domain.{ArrivalType, DeliveryStatus, MessageFilterBy, PaymentType, Role, UserFilterBy}
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Gen._
import org.scalacheck.{Arbitrary, Gen}
import workout.utils.Generators.{fileType, nonEmptyStringGen, numberGen}

import java.time.LocalDateTime

object Arbitraries {

  implicit lazy val arbArrivalType: Arbitrary[ArrivalType] = Arbitrary(oneOf(ArrivalType.arrivalTypes))

  implicit lazy val arbDeliveryStatusType: Arbitrary[DeliveryStatus] = Arbitrary(oneOf(DeliveryStatus.statuses))

  implicit lazy val arbPaymentType: Arbitrary[PaymentType] = Arbitrary(oneOf(PaymentType.paymentTypes))

  implicit lazy val arbMessageFilterType: Arbitrary[MessageFilterBy] = Arbitrary(oneOf(MessageFilterBy.types))

  implicit lazy val arbUserFilterType: Arbitrary[UserFilterBy] = Arbitrary(oneOf(UserFilterBy.types))

  implicit lazy val arbRole: Arbitrary[Role] = Arbitrary(oneOf(Role.roles))

  implicit lazy val arbFilePath: Arbitrary[FilePath] = Arbitrary(
    for {
      s1 <- uuid
      s2 <- fileType
    } yield FilePath.unsafeFrom(s"$s1.$s2")
  )

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

  implicit lazy val arbNonEmptyString: Arbitrary[NonEmptyString] = Arbitrary(
    nonEmptyStringGen(4, 15).map(NonEmptyString.unsafeFrom)
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
      s1 <- fileType
    } yield FileName.unsafeFrom(s"$s0.$s1")
  )

  implicit lazy val arbTel: Arbitrary[Tel] = Arbitrary(
    for {
      s0 <- numberGen(12)
    } yield Tel.unsafeFrom(s"+$s0")
  )

  implicit lazy val arbValidationCode: Arbitrary[ValidationCode] = Arbitrary(
    for {
      s0 <- numberGen(4)
    } yield ValidationCode.unsafeFrom(s"$s0")
  )
}
