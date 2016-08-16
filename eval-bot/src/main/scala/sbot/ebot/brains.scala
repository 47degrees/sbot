/*
 * Scala Bot [eval-bot]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.ebot

import sbot.common.minidef._

import scala.Nil
import scala.::
import scala.Option
import scala.None
import scala.collection.immutable.List
import scala.Predef.refArrayOps
import scala.Predef.augmentString
import sbot.slack.api.data

import cats.syntax.option._

import sbot.slack.api.data._
import sbot.slack.api.web._
import sbot.slack.api.util.SlackQuoteCleaner

/** This data structure holds everything that the bot "knows". */
case class Memory(
  self: User,
  users: List[User],
  channels: List[Channel]
)

object Memory {
  def from(start: WebOp.RTM.Start.Resp): Memory =
    Memory(
      self     = start.self,
      users    = start.users,
      channels = start.channels)
}

case class Logic(
    api: BotApi,
    requiredMessagePrefix: String
) {

  /** Think about a top level event */
  def think(
    memory: Memory,
    event: Event.RTM
  ): Option[BotApi.IO[_]] = event match {

    case Event.Message(message) if !shouldIgnore(memory, message) ⇒
      val text = message.text.stripPrefix(requiredMessagePrefix).trim
      if (text.startsWith("!"))
        thinkCommand(memory, message, text.stripPrefix("!").split(" ").toList)
      else
        None

    case _ ⇒
      println("Received Event: " + event)
      None
  }

  /** Ignore messages from myself, and also check for the required (configurable)
    * message prefix (so we can run multiple bot instances at once).
    */
  private[this] def shouldIgnore(memory: Memory, message: Message.RTM): Boolean =
    message.user == memory.self.id || !message.text.startsWith(requiredMessagePrefix)

  implicit def toSomeIO[A](io: BotApi.IO[A]): Option[BotApi.IO[A]] = io.some

  /** Think about a "Command" sequence */
  private[this] def thinkCommand(
    memory: Memory,
    message: Message.Any,
    command: List[String]
  ): Option[BotApi.IO[_]] =
    command match {

      case "info" :: "user" :: target :: Nil ⇒

        val searchName = target.stripPrefix("@")
        val resp = memory.users.find(_.name == searchName)
          .map(user ⇒
            (
              s"*id:* ${user.id}" ::
              user.profile.toList.flatMap(profile ⇒ List(
                profile.firstName.map("*first name:* " + _),
                profile.lastName.map("*last name:* " + _),
                profile.realName.map("*real name:* " + _),
                profile.email.map("*email:* " + _),
                profile.phone.map("*phone:* " + _)
              ).flatten)
            ).mkString(", "))
          .getOrElse("I can't find that user!")

        for {
          _ ← api.rtm.emitTyping(message.channel)
          _ ← api.debug.trace(s"lookup info on user $target")
          _ ← api.web.chat.postMessage(
            message.channel,
            resp
          )
        } yield ()

      case "think" :: Nil ⇒
        for {
          _ ← api.rtm.emitTyping(message.channel)
          _ ← api.debug.delay(1500)
          _ ← api.web.chat.postMessage(
            message.channel, "I can't think of anything")
        } yield ()

      case "eval" :: tail ⇒

        val rawCode = tail.mkString(" ").trim
        val strippedCode = if (rawCode.startsWith("```")) {
          val code = rawCode.stripPrefix("```")
          if (code.endsWith("```")) code.stripSuffix("```")
          else code
        } else if (rawCode.startsWith("`")) {
          val code = rawCode.stripPrefix("`")
          if (code.endsWith("`")) code.stripSuffix("`")
          else rawCode
        } else rawCode

        val code = SlackQuoteCleaner.clean(strippedCode)

        for {
          _ ← api.debug.trace(s"asked to eval $code")
          _ ← api.rtm.emitTyping(message.channel)
          resp ← api.eval.eval(code)
          _ ← api.debug.trace(s"eval response $resp")
          _ ← api.web.chat.postMessage(
            message.channel,
            (resp.msg ::
              resp.value.map(value ⇒ s"> $value").toList
            ).mkString("\n"))
        } yield ()

      case _ ⇒

        api.web.chat.postMessage(
          message.channel,
          "I don't understand!")
    }

}
