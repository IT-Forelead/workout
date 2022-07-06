package workout.stub_services

import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types
import com.itforelead.workout.services.MessageBroker
import eu.timepit.refined.types.string.NonEmptyString

class MessageBrokerMock[F[_]] extends MessageBroker[F] {
  override def send(messageId: types.MessageId, phone: Tel, text: String): F[Unit] = ???

  override def sendSMSWithoutMember(phone: Tel, text: NonEmptyString): F[Unit] = ???
}
