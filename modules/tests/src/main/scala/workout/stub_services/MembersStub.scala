package workout.stub_services

import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.domain.types.{MemberId, UserId}
import com.itforelead.workout.services.Members

import java.time.LocalDateTime

class MembersStub[F[_]] extends Members[F] {

  override def get(userId: UserId): F[List[Member]] = ???

  override def findMemberByPhone(phone: Tel): F[Option[Member]] = ???

  override def findByUserId(userId: UserId, page: Int): F[Member.MemberWithTotal] = ???

  override def sendValidationCode(userId: UserId, tel: Tel): F[Unit] = ???

  override def validateAndCreate(userId: UserId, createMember: CreateMember, key: FileKey): F[Member] = ???
  override def findActiveTimeShort: F[List[Member]] = ???
  override def findMemberById(memberId: MemberId): F[Option[Member]] = ???
  override def updateActiveTime(memberId: MemberId, activeTime: LocalDateTime): F[Member] = ???
}
