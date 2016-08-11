/*
 * Scala Bot [eval-bot]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.client.slack

import sbot.common.minidef._

import scala.None
import scala.Option
import scala.Some
import scala.collection.immutable.List
import scala.concurrent.duration.FiniteDuration
import scala.sys

import sbot.common.config.semiauto._
import sbot.common.http._
import sbot.slack.api.data._
import sbot.slack.api.web._
import sbot.slack.api.rtm._

import cats.syntax.option._

import knobs._
import java.io.File
import java.nio.file.Paths

import fs2.Strategy
import fs2.interop.cats._

import scalaz.concurrent.{ Task ⇒ ZTask }

/** Slack eval bot configuration */
case class SlackClientConfig(
  token: String,
  entityTimeout: FiniteDuration
)

object SlackClientConfig {
  val reader = deriveKnobsDecoder[SlackClientConfig]
}

/** Slack eval bot
  *
  * @author Andy Scott [47 Degrees]
  */
object SlackClientMain {

  def main(args: scala.Array[String]) {

    val defaults = ClassPathResource("sbot-defaults.cfg")
    val envResource: Resource[Unit] = new Resource[Unit] {
      def resolve(r: Unit, child: Path): Unit = r
      def load(path: Worth[Unit]): ZTask[List[Directive]] =
        ZTask.now(
          sys.env.get("SLACK_TOKEN").map(
          token ⇒ Bind("token", CfgText(token))).toList)
    }

    val sources = List(
      SysPropsResource(Prefix("sbot")).some,
      FileResource(new File("sbot.cfg")).some,
      sys.props.get("user.home").map(home ⇒
        FileResource(Paths.get(home, ".sbot", "sbot.cfg").toFile)),
      Resource.box(())(envResource).some)

    knobs
      .loadImmutable(
        sources.flatten.map(source ⇒ Optional(source or defaults)))
      .map(SlackClientConfig.reader)
      .run.fold(
        e ⇒ System.err.println(s"Error! $e"),
        botMain)

  }

  def botMain(config: SlackClientConfig): Unit = {

    val http = AkkaHttpPie(
      config.entityTimeout, Strategy.fromCachedDaemonPool())
    val interpreter = new DefaultWebOpInterpreter(config.token, http)
    val taskAPI = WebOps.task(interpreter)
    val api = WebOps.free

    def logic(context: WebOp.RTM.Start.Resp): Event.RTM ⇒ Option[api.IO[_]] = {
      case Event.Message(msg) if msg.text.toLowerCase.contains("ping") ⇒
        api.chat.postMessage(
          msg.channel,
          "pong!"
        ).some

      case Event.Message(msg) if msg.user != context.self.id ⇒
        if (msg.text.contains(context.self.name))
          api.chat.postMessage(
            msg.channel,
            "You rang?").some
        else None

      case _ ⇒ None
    }

    val robot = for {
      start ← taskAPI.rtm.start()
      _ ← RTM(http).begin(start)
        .map(logic(start))
        .collect { case Some(op) ⇒ op }
        .evalMap(_.foldMap(interpreter).attempt)
        .run
    } yield ()

    robot.unsafeRun()

  }

}
