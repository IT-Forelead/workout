package workout.http.routes

import com.itforelead.workout.routes.{genFileKey, getFileType, nameToContentType}
import eu.timepit.refined.cats.refTypeShow
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import workout.utils.Generators.{FileTypes, filenameGen}

import java.util.UUID
import scala.util.Try

object UtilsSuite extends SimpleIOSuite with Checkers {

  test("Retrieve content type") {
    forall(filenameGen) { filename =>
      expect(nameToContentType(filename).isDefined)
    }
  }

  test("Retrieve file type") {
    forall(filenameGen) { filename =>
      expect(FileTypes.contains(getFileType(filename)))
    }
  }

  test("Retrieve file key") {
    forall(filenameGen) { filename =>
      genFileKey(filename).map { key =>
        val Array(uuid, fType) = key.value.split('.')
        expect.all(Try(UUID.fromString(uuid)).isSuccess, FileTypes.contains(fType))
      }
    }
  }
}
