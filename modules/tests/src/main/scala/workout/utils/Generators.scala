package workout.utils

import com.itforelead.workout.domain.User._
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.refinements._
import com.itforelead.workout.domain.types._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import Arbitraries._
import com.itforelead.workout.domain.Arrival.{ArrivalFilter, ArrivalMemberId, CreateArrival}
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.Message.{CreateMessage, MessagesFilter}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentFilter, PaymentMemberId}
import com.itforelead.workout.domain.UserSetting.UpdateSetting
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Gen.{oneOf, option}
import squants.Money

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

object Generators {
  val FileTypes: List[String] = List("png","jpg","jpeg","bmp","webp")
  def nonEmptyStringGen(min: Int, max: Int): Gen[String] =
    Gen
      .chooseNum(min, max)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  def numberGen(length: Int): Gen[String] = Gen.buildableOfN[String, Char](length, Gen.numChar)

  def idGen[A](f: UUID => A): Gen[A] =
    Gen.uuid.map(f)

  val fileType: Gen[String] = oneOf(FileTypes)

  val defaultUserId: UserId = UserId(UUID.fromString("76c2c44c-8fbf-4184-9199-19303a042fa0"))

  val defaultFileKey: FileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")

  val userIdGen: Gen[UserId] = idGen(UserId.apply)

  val memberIdGen: Gen[MemberId] = idGen(MemberId.apply)

  val arrivalIdGen: Gen[ArrivalId] = idGen(ArrivalId.apply)

  val messageIdGen: Gen[MessageId] = idGen(MessageId.apply)

  val paymentIdGen: Gen[PaymentId] = idGen(PaymentId.apply)

  val firstNameGen: Gen[FirstName] = arbitrary[NonEmptyString].map(FirstName.apply)

  val gymNameGen: Gen[GymName] = arbitrary[NonEmptyString].map(GymName.apply)

  val lastNameGen: Gen[LastName] = arbitrary[NonEmptyString].map(LastName.apply)

  val textGen: Gen[MessageText] = arbitrary[NonEmptyString].map(MessageText.apply)

  val arrivalTypeGen: Gen[ArrivalType] = arbitrary[ArrivalType]

  val messageFilterTypeGen: Gen[MessageFilterBy] = arbitrary[MessageFilterBy]

  val deliveryStatusGen: Gen[DeliveryStatus] = arbitrary[DeliveryStatus]

  val paymentTypeGen: Gen[PaymentType] = arbitrary[PaymentType]

  val phoneGen: Gen[Tel] = arbitrary[Tel]

  val timestampGen: Gen[LocalDateTime] = arbitrary[LocalDateTime]

  val dateGen: Gen[LocalDate] = timestampGen.map(_.toLocalDate)

  val passwordGen: Gen[Password] = arbitrary[Password]

  val filePathGen: Gen[FilePath] = arbitrary[FilePath]

  val validationCodeGen: Gen[ValidationCode] = arbitrary[ValidationCode]

  val booleanGen: Gen[Boolean] = arbitrary[Boolean]

  val filenameGen: Gen[FileName] = arbitrary[FileName]

  val priceGen: Gen[Money] = Gen.posNum[Long].map(n => UZS(BigDecimal(n)))

  val validationGen: Gen[Validation] = phoneGen.map(Validation.apply)

  val userGen: Gen[User] =
    for {
      i  <- userIdGen
      fn <- firstNameGen
      ln <- lastNameGen
      ph <- phoneGen
    } yield User(i, fn, ln, ph)

  def userSettingGen(userId: Option[UserId] = None): Gen[UserSetting] =
    for {
      ui <- userIdGen
      gName <- gymNameGen
      dPrice <- priceGen
      mPrice <- priceGen
    } yield UserSetting(userId.getOrElse(ui), gName, dPrice, mPrice)

  def updateSettingGen: Gen[UpdateSetting] =
    for {
      gName <- gymNameGen
      dPrice <- priceGen
      mPrice <- priceGen
    } yield UpdateSetting(gName, dPrice, mPrice)

  val createUserGen: Gen[CreateUser] =
    for {
      fn <- firstNameGen
      ln <- lastNameGen
      ph <- phoneGen
      p  <- passwordGen
    } yield CreateUser(fn, ln, ph, p)

  val memberGen: Gen[Member] =
    for {
      i  <- memberIdGen
      ui <- userIdGen
      fn <- firstNameGen
      ln <- lastNameGen
      ph <- phoneGen
      d  <- dateGen
      at <- timestampGen
      im <- filePathGen
    } yield Member(i, ui, fn, ln, ph, d, at, im)

  def createMemberGen(phoneOpt: Option[Tel] = None): Gen[CreateMember] =
    for {
      fn <- firstNameGen
      ln <- lastNameGen
      ph <- phoneGen
      d  <- dateGen
      vc <- validationCodeGen
    } yield CreateMember(fn, ln, phoneOpt.getOrElse(ph), d, vc)

  val arrivalGen: Gen[Arrival] =
    for {
      i  <- arrivalIdGen
      ui <- userIdGen
      mi <- memberIdGen
      dt <- timestampGen
      at <- arrivalTypeGen
    } yield Arrival(i, ui, mi, dt, at)

  val arrivalFilterGen: Gen[ArrivalFilter] =
    for {
      t <- option(arrivalTypeGen)
      f <- option(timestampGen)
      to <- option(timestampGen)
    } yield ArrivalFilter(t, f, to)

  val messageFilterGen: Gen[MessagesFilter] =
    for {
      t <- option(messageFilterTypeGen)
      f <- option(timestampGen)
      to <- option(timestampGen)
    } yield MessagesFilter(t, f, to)

  val createArrivalGen: Gen[CreateArrival] =
    for {
      mi <- memberIdGen
      at <- arrivalTypeGen
    } yield CreateArrival(mi, at)

  val messageGen: Gen[Message] =
    for {
      i  <- messageIdGen
      ui <- userIdGen
      mi <- option(memberIdGen)
      t  <- textGen
      dt <- timestampGen
      ds <- deliveryStatusGen
    } yield Message(i, ui, mi, t, dt, ds)

  def createMessageGen(userId: Option[UserId] = None): Gen[CreateMessage] =
    for {
      ui <- userIdGen
      mi <- option(memberIdGen)
      t  <- textGen
      dt <- timestampGen
      ds <- deliveryStatusGen
    } yield CreateMessage(userId.getOrElse(ui), mi, t, dt, ds)

  val userCredentialGen: Gen[Credentials] =
    for {
      e <- phoneGen
      p <- passwordGen
    } yield Credentials(e, p)

  val paymentGen: Gen[Payment] =
    for {
      i  <- paymentIdGen
      ui <- userIdGen
      mi <- memberIdGen
      pt <- paymentTypeGen
      p  <- priceGen
      ca <- timestampGen
    } yield Payment(i, ui, mi, pt, p, ca)

  val paymentFilterGen: Gen[PaymentFilter] =
    for {
      t <- option(paymentTypeGen)
      f <- option(timestampGen)
      to <- option(timestampGen)
    } yield PaymentFilter(t, f, to)

  val paymentMemberIdGen: Gen[PaymentMemberId] = memberIdGen.map(PaymentMemberId.apply)

  val arrivalMemberIdGen: Gen[ArrivalMemberId] = memberIdGen.map(ArrivalMemberId.apply)

  val createPaymentGen: Gen[CreatePayment] =
    for {
      mi <- memberIdGen
      pt <- paymentTypeGen
    } yield CreatePayment(mi, pt)
}
