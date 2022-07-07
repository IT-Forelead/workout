package workout.stub_services

import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{Member, types}
import com.itforelead.workout.services.Members

class MembersMock[F[_]] extends Members[F] {
  override def findByUserId(userId: types.UserId, page: Int): F[Member.MemberWithTotal]                      = ???
  override def findMemberByPhone(phone: Tel): F[Option[Member]]                                              = ???
  override def sendValidationCode(userId: UserId, phone: Tel): F[Unit]                                       = ???
  override def validateAndCreate(userId: UserId, createMember: Member.CreateMember, key: FileKey): F[Member] = ???
}
