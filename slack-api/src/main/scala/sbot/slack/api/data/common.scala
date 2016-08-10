/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api
package data

import sbot.common.minidef._

import scala.Option

import io.circe.Decoder
import io.circe.generic.semiauto._

case class UserSetValue(
  value: String,
  creator: UserId,
  lastSet: Long
)

object UserSetValue {
  implicit val decodeUserSetValue: Decoder[UserSetValue] =
    Decoder.forProduct3(
      "value", "creator", "last_set"
    )(UserSetValue.apply _)
}

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

case class User(
  id: UserId,
  name: String
)

object User {
  implicit val decodeUser: Decoder[User] =
    deriveDecoder[User]
}
