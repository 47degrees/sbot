/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api
package data

import sbot.common.minidef._

import io.circe.Decoder
import io.circe.generic.semiauto._

sealed trait Message[+S <: Supports] {
  def ts: TS
  def user: UserId
  def channel: ChannelId
  def text: String
}

object Message extends MessageDecoders {

  type RTM = Message[Supports.RTM]
  type EventsAPI = Message[Supports.EventsAPI]
  type Web = Message[Supports.Web]
  type Any = Message[_]

  type All = Message[Supports.RTM with Supports.EventsAPI with Supports.Web]

  case class Default(
    ts: TS,
    user: UserId,
    channel: ChannelId,
    text: String
  ) extends All

}

sealed trait MessageDecoders { self: Message.type ⇒

  private val decodeDefault: Decoder[Default] =
    deriveDecoder[Default]

  implicit val decodeRTM: Decoder[Message.RTM] =
    decodeDefault.map(m ⇒ m: Message.RTM)

  implicit val decodeWeb: Decoder[Message.Web] =
    decodeDefault.map(m ⇒ m: Message.Web)
}
