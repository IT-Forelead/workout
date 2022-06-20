package workout.http.routes

import eu.timepit.refined.cats.refTypeShow
import org.http4s.MediaType.allMediaTypes
import com.itforelead.workout.routes.{getFileType, nameToContentType}
import workout.utils.Generators.filenameGen
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object UtilsSuite extends SimpleIOSuite with Checkers {
  test("Retrieve content type") {
    forall(filenameGen) { filename =>
      expect(nameToContentType(filename).isDefined)
    }
  }

  test("Retrieve file type") {
    forall(filenameGen) { filename =>
      expect(allMediaTypes.flatMap(_.fileExtensions).contains(getFileType(filename)))
    }
  }
}
