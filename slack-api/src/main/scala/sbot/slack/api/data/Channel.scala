/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api
package data

import sbot.common.minidef._

import scala.Option

import io.circe.Decoder

case class Channel(
  id: ChannelId,
  name: String,
  isChannel: Boolean,
  created: Long,
  creator: UserId,
  isArchived: Boolean,
  isGeneral: Boolean,
  topic: Option[Channel.Topic],
  purpose: Option[Channel.Purpose]
)

object Channel {
  type Topic = UserSetValue
  type Purpose = UserSetValue

  implicit val decodeChannel: Decoder[Channel] =
    Decoder.forProduct9(
      "id", "name", "is_channel", "created", "creator",
      "is_archived", "is_general", "topic", "purpose"
    )(Channel.apply _)
}
