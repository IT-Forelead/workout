package workout.http.routes
import cats.effect.IO
import com.itforelead.workout.domain
import com.itforelead.workout.routes.{RefinedRequestDecoder, deriveEntityEncoder}
import io.circe.{Json, JsonObject}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import workout.utils.Generators.createMessageGen

object CommonSuite extends SimpleIOSuite with Checkers with Http4sDsl[IO] {
  test("Request decode unprocessable entity - [ FAIL ]") {
    val body = JsonObject.fromMap(Map("phone" -> Json.fromString("+998901234567")))
    Request[IO](POST, uri"/auth/user")
      .withEntity(body)
      .decodeR[domain.Credentials] { _ =>
        Ok("Success")
      }
      .map { res =>
        assert.same(res.status, UnprocessableEntity)
      }
  }

  test("Request decode incorrect predicate - [ FAIL ]") {
    val body =
      JsonObject.fromMap(Map("phone" -> Json.fromString("bad phone"), "password" -> Json.fromString("bad password")))

    Request[IO](POST, uri"/auth/user")
      .withEntity(body)
      .decodeR[domain.Credentials] { _ =>
        Ok("Success")
      }
      .map { res =>
        assert.same(res.status, BadRequest)
      }
  }

  test("Request decode correct entity - [ SUCCESS ]") {
    val body =
      JsonObject.fromMap(Map("phone" -> Json.fromString("+998901234567"), "password" -> Json.fromString("Secret1!")))

    Request[IO](POST, uri"/auth/user")
      .withEntity(body)
      .decodeR[domain.Credentials] { _ =>
        Ok("Success")
      }
      .map { res =>
        assert.same(res.status, Ok)
      }
  }
}
