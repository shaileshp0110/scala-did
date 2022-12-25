package fmgp.did.comm.protocol.basicmessage2

import zio.json._

import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toBasicMessage: Either[String, BasicMessage] =
    BasicMessage.fromPlaintextMessage(msg)

/** The message message is sent by the sender to the recipient.
  *
  * Note that the role is only specific to the creation of messages, and that both parties may play both roles.
  *
  * {{{
  *  {
  * "id": "123456780",
  * "type": "https://didcomm.org/basicmessage/2.0/message",
  * "lang": "en",
  * "created_time": 1547577721,
  * "body": {
  * "content": "Your hovercraft is full of eels."
  * }
  * }
  * }}}
  *
  * @param lang
  *   See [https://identity.foundation/didcomm-messaging/spec/#internationalization-i18n]
  */
final case class BasicMessage(
    id: MsgID = MsgID(),
    lang: NotRequired[String] = None,
    created_time: NotRequired[UTCEpoch] = None,
    content: String,
) {
  def `type` = BasicMessage.piuri

  def toPlaintextMessage(from: Option[DIDSubject], to: Set[DIDSubject]): Either[String, PlaintextMessage] =
    BasicMessage
      .Body(content)
      .toJsonAST
      .map(body =>
        PlaintextMessageClass(
          id = id,
          `type` = `type`,
          to = Some(to), // NotRequired[Set[DIDSubject]],
          from = from, // NotRequired[DIDSubject],
          thid = None, // NotRequired[MsgID],
          created_time = created_time, // NotRequired[UTCEpoch],
          expires_time = None, // NotRequired[UTCEpoch],
          body = body, // Required[JSON_RFC7159],
          attachments = None, // NotRequired[Seq[Attachment]]
          // FIXME lang: NotRequired[String] = lang,
        )
      )
}

object BasicMessage {
  def piuri = PIURI("https://didcomm.org/basicmessage/2.0/message")

  protected final case class Body(content: String)
  object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, BasicMessage] = {
    if (msg.`type` != BasicMessage.piuri)
      Left(s"No able to create BasicMessage from a Message of the type '${msg.`type`}'")
    else
      msg.body.as[Body].map { body =>
        BasicMessage(
          id = msg.id,
          lang = None, // TODO FIXME
          created_time = msg.created_time,
          content = body.content
        )
      }
  }
}