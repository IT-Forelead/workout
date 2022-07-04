package workout.stub_services

import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.Members

class MembersStub[F[_]] extends Members[F] {
  override def create(memberParam: CreateMember): F[Member] = ???
  override def findByUserId(userId: UserId): F[List[Member]] = ???
  override def findMemberByPhone(phone: Tel): F[Option[Member]] = ???
}
