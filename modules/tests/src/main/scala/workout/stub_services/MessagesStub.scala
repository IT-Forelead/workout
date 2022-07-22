package workout.stub_services

import com.itforelead.workout.domain.{DeliveryStatus, Message, types}
import com.itforelead.workout.domain.Message.{CreateMessage, MessageWithMember, MessagesFilter}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.Messages

class MessagesStub[F[_]] extends Messages[F] {
  override def create(msg: CreateMessage): F[Message] = ???
  override def get(userId: UserId): F[List[MessageWithMember]] = ???
  override def sendValidationCode(userId: UserId, tel: Tel): F[Unit] = ???
  override def sentSMSTodayMemberIds: F[List[types.MemberId]] = ???
  override def getMessagesWithTotal(userId: UserId, filter: MessagesFilter, page: Int): F[Message.MessageWithTotal] = ???
  override def changeStatus(id: types.MessageId, status: DeliveryStatus): F[Message] = ???
}
