/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api
package data

import sbot.common.minidef._

import io.circe.Decoder
import io.circe.generic.semiauto._

sealed trait Message {
  def ts: TS
  def user: UserId
  def text: String
}

object Message extends MessageDecoders {

  trait WithChannel { self: Message ⇒
    def channel: ChannelId
  }

  type RTM = DefaultWithChannel
  type EventsAPI = Default
  type Web = Default

  case class Default(
    ts: TS,
    user: UserId,
    text: String
  ) extends Message

  case class DefaultWithChannel(
    ts: TS,
    user: UserId,
    channel: ChannelId,
    text: String
  ) extends Message with WithChannel

}

sealed trait MessageDecoders { self: Message.type ⇒

  implicit val decodeDefault: Decoder[Default] =
    deriveDecoder[Default]

  implicit val decodeDefaultWithChannel: Decoder[DefaultWithChannel] =
    deriveDecoder[DefaultWithChannel]
}
