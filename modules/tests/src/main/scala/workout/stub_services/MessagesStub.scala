package workout.stub_services

import com.itforelead.workout.domain.{DeliveryStatus, Message, types}
import com.itforelead.workout.domain.Message.{CreateMessage, MessageWithMember}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.Messages

class MessagesStub[F[_]] extends Messages[F] {
  override def create(msg: CreateMessage): F[Message] = ???
  override def get(userId: UserId): F[List[MessageWithMember]] = ???
  override def changeStatus(id: types.MessageId, status: DeliveryStatus): F[Message] = ???
}
