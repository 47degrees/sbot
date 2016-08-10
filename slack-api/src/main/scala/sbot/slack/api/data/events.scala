/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api
package data

import sbot.common.minidef._

import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.generic.semiauto._

import cats.syntax.xor._

import data.{ Message ⇒ Message_ }

sealed trait Event[+S <: Supports]

object Event extends EventDecoders {

  type RTM = Event[Supports.RTM]
  type EventsAPI = Event[Supports.EventsAPI]
  type Both = Event[Supports.RTM with Supports.EventsAPI]

  //

  case object AccountsChanged extends RTM

  //

  case class BotAdded() extends RTM
  case class BotChanged() extends RTM

  //

  case class ChannelArchive() extends Both
  case class ChannelCreated() extends Both
  case class ChannelDeleted() extends Both
  case class ChannelHistoryChanged() extends Both
  case class ChannelJoined(
    channel: Channel
  ) extends Both

  case class ChannelLeft() extends RTM
  case class ChannelMarked() extends RTM
  case class ChannelRename() extends Both
  case class ChannelUnarchive() extends Both

  //

  case object Hello extends RTM

  //

  case class Message(
    message: Message_.RTM
  ) extends RTM

  //

  case class PresenceChange(
    user: UserId,
    presence: String
  ) extends RTM

  //

  case class ReactionAdded(
    user: UserId,
    reaction: String
  ) extends Both

  //

  case class ReconnectUrl(
    url: String
  ) extends RTM

  //

  case class UserTyping(
    user: UserId,
    channel: ChannelId
  ) extends RTM

}

sealed trait EventDecoders { self: Event.type ⇒

  implicit val decodeRTM: Decoder[Event.RTM] = Decoder.instance(c ⇒
    c.downField("type").as[String].flatMap {

      case "accounts_changed"        ⇒ AccountsChanged.right

      case "bot_added"               ⇒ decodeBotAdded(c)
      case "bot_changed"             ⇒ decodeBotChanged(c)

      case "channel_archive"         ⇒ decodeChannelArchive(c)
      case "channel_created"         ⇒ decodeChannelCreated(c)
      case "channel_deleted"         ⇒ decodeChannelDeleted(c)
      case "channel_history_changed" ⇒ decodeChannelHistoryChanged(c)
      case "channel_joined"          ⇒ decodeChannelJoined(c)
      case "channel_left"            ⇒ decodeChannelLeft(c)
      case "channel_marked"          ⇒ decodeChannelMarked(c)
      case "channel_rename"          ⇒ decodeChannelRename(c)
      case "channel_unarchive"       ⇒ decodeChannelUnarchive(c)

      case "hello"                   ⇒ Hello.right
      case "message"                 ⇒ decodeMessage(c)
      case "presence_change"         ⇒ decodePresenceChange(c)
      case "reaction_added"          ⇒ decodeReactionAdded(c)
      case "reconnect_url"           ⇒ decodeReconnectUrl(c)
      case "user_typing"             ⇒ decodeUserTyping(c)

      case other ⇒ DecodingFailure(
        s"Unknown/handled type $other", c.history).left
    }
  )

  implicit val decodeEventsAPI: Decoder[Event.EventsAPI] = Decoder.instance(c ⇒
    c.downField("type").as[String].flatMap {
      case "channel_archive"         ⇒ decodeChannelArchive(c)
      case "channel_created"         ⇒ decodeChannelCreated(c)
      case "channel_deleted"         ⇒ decodeChannelDeleted(c)
      case "channel_history_changed" ⇒ decodeChannelHistoryChanged(c)
      case "channel_joined"          ⇒ decodeChannelJoined(c)
      case "channel_rename"          ⇒ decodeChannelRename(c)
      case "channel_unarchive"       ⇒ decodeChannelUnarchive(c)

      case "reaction_added"          ⇒ decodeReactionAdded(c)

      case other ⇒ DecodingFailure(
        s"Unknown/handled type $other", c.history).left
    }
  )

  private val decodeBotAdded: Decoder[BotAdded] =
    deriveDecoder[BotAdded]
  private val decodeBotChanged: Decoder[BotChanged] =
    deriveDecoder[BotChanged]

  private val decodeChannelArchive: Decoder[ChannelArchive] =
    deriveDecoder[ChannelArchive]
  private val decodeChannelCreated: Decoder[ChannelCreated] =
    deriveDecoder[ChannelCreated]
  private val decodeChannelDeleted: Decoder[ChannelDeleted] =
    deriveDecoder[ChannelDeleted]
  private val decodeChannelHistoryChanged: Decoder[ChannelHistoryChanged] =
    deriveDecoder[ChannelHistoryChanged]
  private val decodeChannelJoined: Decoder[ChannelJoined] =
    deriveDecoder[ChannelJoined]
  private val decodeChannelLeft: Decoder[ChannelLeft] =
    deriveDecoder[ChannelLeft]
  private val decodeChannelMarked: Decoder[ChannelMarked] =
    deriveDecoder[ChannelMarked]
  private val decodeChannelRename: Decoder[ChannelRename] =
    deriveDecoder[ChannelRename]
  private val decodeChannelUnarchive: Decoder[ChannelUnarchive] =
    deriveDecoder[ChannelUnarchive]

  private val decodeMessage: Decoder[Message] =
    Decoder[Message_.RTM].map(Message(_))

  private val decodePresenceChange: Decoder[PresenceChange] =
    deriveDecoder[PresenceChange]

  private val decodeReactionAdded: Decoder[ReactionAdded] =
    deriveDecoder[ReactionAdded]

  private val decodeReconnectUrl: Decoder[ReconnectUrl] =
    deriveDecoder[ReconnectUrl]

  private val decodeUserTyping: Decoder[UserTyping] =
    deriveDecoder[UserTyping]

}
