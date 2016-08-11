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

case class User(
  id: UserId,
  name: String,
  deleted: Option[Boolean],
  color: Option[String],
  profile: Option[User.Profile]
)

object User {

  case class Profile(
    firstName: Option[String],
    lastName: Option[String],
    realName: Option[String],
    email: Option[String],
    skype: Option[String],
    phone: Option[String],
    image24: Option[String],
    image32: Option[String],
    image48: Option[String],
    image72: Option[String],
    image192: Option[String],
    image512: Option[String]
  )

  private implicit val decodeProfile: Decoder[Profile] =
    Decoder.forProduct12(
      "first_name",
      "last_name",
      "real_name",
      "email",
      "skype",
      "phone",
      "image_24",
      "image_32",
      "image_48",
      "image_72",
      "image_192",
      "image_512"
    )(Profile.apply _)

  implicit val decodeUser: Decoder[User] =
    deriveDecoder[User]
}
