package workout.stub_services

import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.{CreateUser, UserWithPassword}
import com.itforelead.workout.domain.custom.refinements.{EmailAddress, Tel}
import com.itforelead.workout.services.Users

class UsersStub[F[_]] extends Users[F] {
  override def find(phoneNumber: Tel): F[Option[UserWithPassword]]                 = ???
  override def create(userParam: CreateUser, password: PasswordHash[SCrypt]): F[User] = ???
}