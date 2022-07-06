package workout.stub_services

import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.{Members, Validations}

class MembersStub[F[_]] extends Members[F] with Validations[F]{
  override def create(memberParam: CreateMember, filePath: FileKey): F[Member] = ???

  override def findMemberByPhone(phone: Tel): F[Option[Member]] = ???

  override def findByUserId(userId: UserId, page: Int): F[Member.MemberWithTotal] = ???

  override def sendValidationCode(phone: Tel): F[Unit] = ???

  override def validatePhone(createMember: CreateMember, key: FileKey): F[Member] = ???
}
