/*
 * Scala Bot [common]
 *  … yet another 🤖 framework, for Scala
 */

package sbot
package common.json

/*

import cats.data.Xor
import cats.syntax.xor._

import io.circe.Decoder
import io.circe.Json
import io.circe.{ parser ⇒ JSONParser }

import org.http4s.websocket.WebsocketBits._

object JsonFrameDecoder {
  sealed trait Failed extends Product with Serializable

  object Failed {
    case class UnexpectedFrame(frame: WebSocketFrame) extends Failed
    case class ToParse(
      raw: String, underlying: String) extends Failed
    case class ToDecode(
      json: Json, underlying: String) extends Failed
  }

  def decode[A: Decoder](frame: WebSocketFrame): Failed Xor A =
    frame match {
      case textFrame: Text ⇒ decodeTextFrame(textFrame)
      case unexpectedFrame ⇒ Failed.UnexpectedFrame(unexpectedFrame).left
    }

  private[this] def decodeTextFrame[A: Decoder](textFrame: Text): Failed Xor A =
    JSONParser
      .parse(textFrame.str)
      .leftMap(e ⇒
        Failed.ToParse(textFrame.str, e.getMessage))
      .flatMap(json ⇒ Decoder[A].decodeJson(json)
        .leftMap(e ⇒
          Failed.ToDecode(json, e.getMessage)))

}
*/
