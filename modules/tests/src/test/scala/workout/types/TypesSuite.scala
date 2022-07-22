package workout.types

import com.itforelead.workout.domain.Role
import org.scalacheck.Gen.oneOf
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.util.{Failure, Success, Try}

object TypesSuite extends SimpleIOSuite with Checkers {

  test("Role find - [ SUCCESS ]") {
    forall(oneOf("admin", "client")) { roleStr =>
      expect(Role.find(roleStr).isDefined)
    }
  }

  test("Role get - [ SUCCESS ]") {
    forall(oneOf("admin", "client")) { roleStr =>
      expect(Role.unsafeFrom(roleStr).isInstanceOf[Role])
    }
  }

  test("Role find - [ Error ]") {
    forall(oneOf("foo", "bar")) { roleStr =>
      expect(Role.find(roleStr).isEmpty)
    }
  }

  test("Role get - [ Error ]") {
    forall(oneOf("foo", "bar")) { roleStr =>
      Try { Role.unsafeFrom(roleStr) } match {
        case Failure(_: IllegalArgumentException) => success
        case Failure(exception) => failure("Should be IllegalArgumentException, but " + exception.getMessage)
        case Success(_)         => failure("Should be IllegalArgumentException")
      }
    }
  }
}
