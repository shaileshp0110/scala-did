package fmgp.did.comm.agent

import zio._
import zio.json._

import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.Operations._
import fmgp.did.comm.protocol.basicmessage2._
import fmgp.did.comm.protocol.trustping2._

//TODO pick a better name // maybe "Protocol" only

trait ProtocolExecuter[-R] {
  def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, DidFail, Unit] = program(plaintextMessage) *> ZIO.unit

  def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, DidFail, Option[PlaintextMessage]]
}

object ProtocolExecuter {

  type Services = Resolver & Agent & Operations & MessageDispatcher

  def getExecuteFor(piuri: PIURI): ProtocolExecuter[Services] = {
    // the val is from the match to be definitely stable
    val piuriBasicMessage = BasicMessage.piuri
    val piuriTrustPing = TrustPing.piuri
    val piuriTrustPingResponse = TrustPingResponse.piuri
    piuri match
      case `piuriBasicMessage`      => BasicMessageExecuter
      case `piuriTrustPing`         => new TrustPingExecuter
      case `piuriTrustPingResponse` => new TrustPingExecuter
      case anyPiuri                 => NullProtocolExecute
  }
}

trait ProtocolExecuterWithServices[-R <: ProtocolExecuter.Services] extends ProtocolExecuter[R] {

  override def execute[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, DidFail, Unit] =
    program(plaintextMessage)
      .tap(v => ZIO.logDebug(v.toJsonPretty)) // DEBUG
      .flatMap {
        case None => ZIO.unit
        case Some(reply) =>
          for {
            msg <- reply.from match
              case Some(value) => authEncrypt(reply)
              case None        => anonEncrypt(reply)
            // TODO forward message
            to <- reply.to match // TODO improve
              case None => ZIO.unit
              case Some(send2DIDs) =>
                ZIO.foreach(send2DIDs)(to =>
                  for {
                    messageDispatcher <- ZIO.service[MessageDispatcher]
                    resolver <- ZIO.service[Resolver]
                    doc <- resolver.didDocument(to)
                    url =
                      doc.service.toSeq.flatten // TODO .filter(_.`type`.contend(DIDService.TYPE_DIDCommMessaging))
                      match {
                        case head +: next =>
                          head.serviceEndpoint match
                            case s: String                      => s
                            case s: Seq[URI] @unchecked         => s.head
                            case s: Map[String, URI] @unchecked => s.head._2
                      }
                    _ <- ZIO.log(s"Send to url: $url")
                    job <- messageDispatcher
                      .send(
                        msg,
                        url, // "http://localhost:8080", // FIXME REMOVE (use for local env)
                        None
                        // url match // FIXME REMOVE (use for local env)
                        //   case http if http.startsWith("http://") => Some(url.drop(7).split(':').head.split('/').head)
                        //   case https if https.startsWith("https://") =>
                        //     Some(url.drop(8).split(':').head.split('/').head)
                        //   case _ => None
                      )
                  } yield ()
                ) *> ZIO.unit
          } yield ()
      }

  override def program[R1 <: R](
      plaintextMessage: PlaintextMessage,
      // context: Context
  ): ZIO[R1, DidFail, Option[PlaintextMessage]]
}

object NullProtocolExecute extends ProtocolExecuter[Any] {
  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) =
    ZIO.fail(MissingProtocol(plaintextMessage.`type`))
}

object BasicMessageExecuter extends ProtocolExecuter[Any] {
  override def program[R1 <: Any](plaintextMessage: PlaintextMessage) = for {
    job <- BasicMessage.fromPlaintextMessage(plaintextMessage) match
      case Left(error) => ZIO.fail(FailToParse(error))
      case Right(bm)   => Console.printLine(bm.toString).mapError(ex => SomeThrowable(ex))
  } yield None
}

class TrustPingExecuter extends ProtocolExecuterWithServices[ProtocolExecuter.Services] {

  override def program[R1 <: Agent](
      plaintextMessage: PlaintextMessage
  ): ZIO[R1, DidFail, Option[PlaintextMessage]] = {
    // the val is from the match to be definitely stable
    val piuriTrustPing = TrustPing.piuri
    val piuriTrustPingResponse = TrustPingResponse.piuri

    plaintextMessage.`type` match
      case `piuriTrustPing` =>
        TrustPing.fromPlaintextMessage(plaintextMessage) match
          case Left(error)                                    => ZIO.fail(FailToParse(error))
          case Right(ping: TrustPingWithOutRequestedResponse) => ZIO.logInfo(ping.toString()) *> ZIO.none
          case Right(ping: TrustPingWithRequestedResponse) =>
            for {
              _ <- ZIO.logInfo(ping.toString())
              agent <- ZIO.service[Agent]
              ret = ping.makeRespond
            } yield Some(ret.toPlaintextMessage(to = ping.from, from = Some(agent.id)))
      case `piuriTrustPingResponse` =>
        for {
          job <- TrustPingResponse.fromPlaintextMessage(plaintextMessage) match
            case Left(error) => ZIO.fail(FailToParse(error))
            case Right(ping) => ZIO.logInfo(ping.toString())
        } yield None
  }

}