package workout.utils

import com.itforelead.workout.domain.User._
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.refinements._
import com.itforelead.workout.domain.types._
import eu.timepit.refined.scalacheck.string._
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import Arbitraries._

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

  val userIdGen: Gen[UserId] =
    idGen(UserId.apply)

  val usernameGen: Gen[UserName] =
    arbitrary[FullName].map(UserName.apply)

  val phoneGen: Gen[Tel] = arbitrary[Tel]

  val timestampGen: Gen[LocalDateTime] = arbitrary[LocalDateTime]

  val dateGen: Gen[LocalDate] = timestampGen.map(_.toLocalDate)

  val passwordGen: Gen[Password] = arbitrary[Password]

  val filePathGen: Gen[FilePath] = arbitrary[FilePath]

  val booleanGen: Gen[Boolean] = arbitrary[Boolean]

  val emailGen: Gen[EmailAddress] = arbitrary[EmailAddress]

  val filenameGen: Gen[FileName] = arbitrary[FileName]

  val roleGen: Gen[Role] = arbitrary[Role]

  val userGen: Gen[User] =
    for {
      i <- userIdGen
      n <- usernameGen
      p <- phoneGen
      b <- timestampGen
      f <- filePathGen
      r <- roleGen
    } yield User(i, n, p, b, f, r)

  val userCredentialGen: Gen[Credentials] =
    for {
      e <- phoneGen
      p <- passwordGen
    } yield Credentials(e, p)

  val createUserGen: Gen[CreateUser] =
    for {
      n <- usernameGen
      p <- phoneGen
      b <- timestampGen
      f <- filePathGen
      r <- roleGen
      ps <- passwordGen
    } yield CreateUser(n, p, b, f, r , ps)
}
