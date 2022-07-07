package workout.utils

import com.itforelead.workout.domain.User._
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.refinements._
import com.itforelead.workout.domain.types._
import eu.timepit.refined.scalacheck.string._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import Arbitraries._
import com.itforelead.workout.domain.Arrival.CreateArrival
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.Message.CreateMessage
import com.itforelead.workout.domain.Payment.CreatePayment
import eu.timepit.refined.scalacheck.all.greaterEqualArbitrary
import eu.timepit.refined.types.numeric.NonNegShort
import eu.timepit.refined.types.string.NonEmptyString
import squants.Money

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

object Generators {

  def nonEmptyStringGen(min: Int, max: Int): Gen[String] =
    Gen
      .chooseNum(min, max)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  def nonEmptyAlphaNumGen(min: Int, max: Int): Gen[String] =
    Gen
      .chooseNum(min, max)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaNumChar)
      }

  def numberGen(length: Int): Gen[String] = Gen.buildableOfN[String, Char](length, Gen.numChar)

  def idGen[A](f: UUID => A): Gen[A] =
    Gen.uuid.map(f)

  val userIdGen: Gen[UserId] = idGen(UserId.apply)

  val memberIdGen: Gen[MemberId] = idGen(MemberId.apply)

  val arrivalIdGen: Gen[ArrivalId] = idGen(ArrivalId.apply)

  val messageIdGen: Gen[MessageId] = idGen(MessageId.apply)

  val paymentIdGen: Gen[PaymentId] = idGen(PaymentId.apply)

  val firstNameGen: Gen[FirstName] = arbitrary[NonEmptyString].map(FirstName.apply)

  val lastNameGen: Gen[LastName] = arbitrary[NonEmptyString].map(LastName.apply)

  val textGen: Gen[MessageText] = arbitrary[NonEmptyString].map(MessageText.apply)

  val arrivalTypeGen: Gen[ArrivalType] = arbitrary[ArrivalType]

  val deliveryStatusGen: Gen[DeliveryStatus] = arbitrary[DeliveryStatus]

  val paymentTypeGen: Gen[PaymentType] = arbitrary[PaymentType]

  val phoneGen: Gen[Tel] = arbitrary[Tel]

  val timestampGen: Gen[LocalDateTime] = arbitrary[LocalDateTime]

  val dateGen: Gen[LocalDate] = timestampGen.map(_.toLocalDate)

  val passwordGen: Gen[Password] = arbitrary[Password]

  val filePathGen: Gen[FilePath] = arbitrary[FilePath]

  val validationCodeGen: Gen[ValidationCode] = arbitrary[ValidationCode]

  val booleanGen: Gen[Boolean] = arbitrary[Boolean]

  val emailGen: Gen[EmailAddress] = arbitrary[EmailAddress]

  val filenameGen: Gen[FileName] = arbitrary[FileName]

  val priceGen: Gen[Money] = Gen.posNum[Long].map(n => UZS(BigDecimal(n)))

  val durationGen: Gen[Duration] = arbitrary[NonNegShort].map(Duration.apply)

  val totalGen: Gen[Long] = arbitrary[Long]

  val userGen: Gen[User] =
    for {
      i <- userIdGen
      fn <- firstNameGen
      ln <- lastNameGen
      ph <- phoneGen
    } yield User(i, fn, ln, ph)

  val createUserGen: Gen[CreateUser] =
    for {
      fn <- firstNameGen
      ln <- lastNameGen
      ph <- phoneGen
      p  <- passwordGen
    } yield CreateUser(fn, ln, ph, p)

  val memberGen: Gen[Member] =
    for {
      i <- memberIdGen
      ui <- userIdGen
      fn <- firstNameGen
      ln <- lastNameGen
      ph <- phoneGen
      d <- dateGen
      im <- filePathGen
    } yield Member(i, ui, fn, ln, ph, d, im)

  val memberWithTotalGen: Gen[MemberWithTotal] =
    for {
     m <- memberGen
     t <- totalGen
    } yield MemberWithTotal(List(m), t)

  val createMemberGen: Gen[CreateMember] =
    for {
      ui <- userIdGen
      fn <- firstNameGen
      ln <- lastNameGen
      ph <- phoneGen
      d  <- dateGen
      vc <- validationCodeGen
    } yield CreateMember(ui, fn, ln, ph, d, vc)

  val arrivalGen: Gen[Arrival] =
    for {
      i  <- arrivalIdGen
      ui <- userIdGen
      mi <- memberIdGen
      dt <- timestampGen
      at <- arrivalTypeGen
    } yield Arrival(i, ui, mi, dt, at)

  val createArrivalGen: Gen[CreateArrival] =
    for {
      ui <- userIdGen
      mi <- memberIdGen
      at <- arrivalTypeGen
    } yield CreateArrival(ui, mi, at)

  val messageGen: Gen[Message] =
    for {
      i  <- messageIdGen
      ui <- userIdGen
      mi <- memberIdGen
      t  <- textGen
      dt <- timestampGen
      ds <- deliveryStatusGen
    } yield Message(i, ui, mi, t, dt, ds)

  val createMessageGen: Gen[CreateMessage] =
    for {
      ui <- userIdGen
      mi <- memberIdGen
      t  <- textGen
      dt <- timestampGen
      ds <- deliveryStatusGen
    } yield CreateMessage(ui, mi, t, dt, ds)

  val userCredentialGen: Gen[Credentials] =
    for {
      e <- phoneGen
      p <- passwordGen
    } yield Credentials(e, p)

  val paymentGen: Gen[Payment] =
    for {
      i <- paymentIdGen
      ui <- userIdGen
      mi <- memberIdGen
      pt <- paymentTypeGen
      p <- priceGen
      ca <- timestampGen
      ea <- timestampGen
    } yield Payment(i, ui, mi, pt, p, ca, ea)

  val createPaymentGen: Gen[CreatePayment] =
    for {
      ui <- userIdGen
      mi <- memberIdGen
      pt <- paymentTypeGen
      p <- priceGen
    } yield CreatePayment(ui, mi, pt, p)
}

