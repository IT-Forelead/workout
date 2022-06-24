package com.itforelead.workout.services

import cats.effect.Sync
import org.typelevel.log4cats.Logger

trait Congratulator[F[_]] {
  def start: F[Unit]
}

object Congratulator {
  def make[F[_]: Sync: Logger](
    messageBroker: MessageBroker[F]
  ): Congratulator[F] =
    new Congratulator[F] {

//
//      private def prepareTextAndSend(contact: Contact): OptionT[F, SMSTemplate] => F[Unit] =
//        _.map(template => template.id -> prepare(template, contact))
//          .cataF(
//            Logger[F].debug(s"Has not selected template id for gender [ ${contact.gender} ]"),
//            { case (templateId, text) =>
//              createMessage(contact.id, templateId).flatMap { message =>
//                send(contact, text, message)
//              }
//            }
//          )
//
//      private def createMessage(contactId: ContactId, templateId: TemplateId): F[Message] =
//        Sync[F].delay(LocalDateTime.now()).flatMap { now =>
//          messages.create(CreateMessage(contactId, templateId, now, DeliveryStatus.SENT))
//        }
//
//      private def prepare(template: SMSTemplate, contact: Contact): String =
//        template.text.value
//          .replace("[FIRSTNAME]", contact.firstName.value)
//          .replace("[LASTNAME]", contact.lastName.value)
//
//      private def send(contact: Contact, text: String, message: Message): F[Unit] =
//        messages.changeStatus(message.id, status = DeliveryStatus.DELIVERED) >>
//          messageBroker.send(message.id, contact.phone, text)

      override def start: F[Unit] = ???
    }

}
