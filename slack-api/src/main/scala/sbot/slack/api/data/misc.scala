/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api
package data

import sbot.common.minidef._

import scala.collection.immutable.List
import scala.collection.immutable.Nil

import io.circe.Decoder
import io.circe.generic.semiauto._

/** This is used for configurable values, such as Channel topics. */
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

// TODO: maybe move to the web package?
/** The generic response type for web API calls */
sealed trait Response[+A]

object Response {

  case class Success[A](
    payload: A,
    warnings: List[String]
  ) extends Response[A]

  case class Error(
    error: String
  ) extends Response[Nothing]

  private val decodeWarnings =
    Decoder[String].map(List(_)) or Decoder[List[String]]

  private val decodeError: Decoder[Error] =
    deriveDecoder[Error]

  implicit def decodeResponse[A: Decoder]: Decoder[Response[A]] =
    Decoder.instance(c ⇒
      c.downField("ok").as[Boolean] flatMap {
        case true ⇒
          for {
            payload ← c.as[A]
            warnings = c.downField("warning").as(decodeWarnings) getOrElse Nil
          } yield Success(payload, warnings)
        case false ⇒
          c.as(decodeError)
      }
    )

}
