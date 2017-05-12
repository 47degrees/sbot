/*
 * Scala Bot [eval-bot]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.ebot

import sbot.common.minidef._

import scala.Some
import scala.concurrent.duration._

import sbot.common.http._
import sbot.slack.api.rtm._
import sbot.slack.api.web._

import cats.~>

import classy.config._
import classy.generic.auto._

import fs2.Task
import fs2.Strategy
import fs2.async
import fs2.interop.cats._

import io.circe.Json

/** Slack eval bot
  *
  * @author Andy Scott [47 Degrees]
  */
object SlackClientMain {

  case class Config(
    requiredMessagePrefix: String,
    http: Config.Http,
    slack: Config.Slack,
    evaluator: Config.Eval
  )

  object Config {
    case class Http(entityTimeout: FiniteDuration)
    case class Slack(token: String)
    case class Eval(token: String, uri: String)
  }

  def main(args: scala.Array[String]): Unit =
    ConfigDecoder[Config].load().fold(
      e ⇒ System.err.println(s"Config Error:\n${e.toPrettyString}"),
      botMain)

  def botMain(config: Config): Unit = {
    import BotApi._

    implicit val strategy = Strategy.fromCachedDaemonPool()

    val http = AkkaHttpPie(config.http.entityTimeout, strategy)
    val webInterpreter = new DefaultWebOpInterpreter(config.slack.token, http)

    val evalBot: Task[Unit] = for {

      // ask Slack for a web socket address
      start ← WebOps(webInterpreter).rtm.start()

      // populate our "memory" with information from the start response
      memory = Memory.from(start)

      // open the web socket to Slack
      (input, output) = RTM(http).begin(start)

      // create a queue, used for writing to the web socket
      // when we evaluate our algebras
      outputQueue ← async.unboundedQueue[Task, Json]
      _ = outputQueue.dequeue.to(output)
        .run.unsafeRunAsync(_ ⇒ ())

      // create the interpreter for our algebras
      evalInterpreter = EvalApi.EvalOp.defaultInterpreter(
        http,
        config.evaluator.uri,
        config.evaluator.token)
      botInterpreter = BotApi.BotOp.defaultInterpreter[Task]
      rtmInterpreter = RtmOp.defaultInterpreter[Task](outputQueue)
      interpreter1 = rtmInterpreter or evalInterpreter: Bot1 ~> Task
      interpreter0 = botInterpreter or interpreter1: Bot0 ~> Task
      interpreter = webInterpreter or interpreter0: Bot ~> Task

      // instantiate our logic
      logic = Logic(BotApi(), config.requiredMessagePrefix)

      // process the input
      _ ← input
        .map(logic.think(memory, _))
        .collect { case Some(op) ⇒ op }
        .map(_.foldMap(interpreter).attempt.unsafeRunAsync(_ ⇒ ()))
        .run

    } yield ()

    evalBot.unsafeRun()

  }

}
