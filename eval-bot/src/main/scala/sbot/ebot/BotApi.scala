/*
 * Scala Bot [eval-bot]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.ebot

import sbot.common.minidef._

import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.MILLISECONDS
import scala.io.StdIn

import sbot.slack.api.rtm._
import sbot.slack.api.web._

import cats.~>
import cats.data.Coproduct
import cats.free.Free
import cats.free.Inject

import fs2.Strategy
import fs2.util.Async

object EvalApi extends EvalApiTypes {

  import fs2.Task
  import sbot.common.http.HttpPie
  import scala.collection.immutable.List

  class EvalOps[F[_]](implicit I: Inject[EvalOp, F]) {
    import EvalOp._

    def eval(code: String): Free[F, EvalResponse] = Free.inject(Eval(code))
  }

  object EvalOps {
    def freeIn[F[_]](implicit I: Inject[EvalOp, F]): EvalOps[F] = new EvalOps[F]
  }

  sealed trait EvalOp[A]
  object EvalOp {
    case class Eval(code: String) extends EvalOp[EvalResponse]

    def defaultInterpreter(
      http: HttpPie,
      evaluatorUri: String,
      evaluatorToken: String) =
      new (EvalOp ~> Task) {
        def apply[A](i: EvalOp[A]) = i match {
          case Eval(code) ⇒

            http.post[EvalRequest, EvalResponse](
              evaluatorUri,
              EvalRequest(
                code = code),
              List (
                "x-scala-eval-api-token" → evaluatorToken))
        }
      }

  }

}

sealed trait EvalApiTypes {

  import io.circe._
  import io.circe.generic.semiauto._
  import scala.collection.immutable.Nil
  import scala.collection.immutable.List
  import scala.collection.immutable.Map
  import scala.Option
  import scala.Predef._

  case class Dependency(
    groupId: String,
    artifactId: String,
    version: String)

  object Dependency {
    implicit val encodeDependency: Encoder[Dependency] =
      deriveEncoder[Dependency]
  }

  case class EvalRequest(
    resolvers: List[String] = Nil,
    dependencies: List[Dependency] = Nil,
    code: String)

  object EvalRequest {
    implicit val encodeEvalRequest: Encoder[EvalRequest] =
      deriveEncoder[EvalRequest]
  }

  case class RangePosition(start: Int, point: Int, end: Int)

  case class CompilationInfo(message: String, pos: Option[RangePosition])

  case class EvalResponse(
    msg: String,
    value: Option[String],
    valueType: Option[String],
    compilationInfos: Map[String, List[CompilationInfo]])

  object EvalResponse {
    private implicit val decodeRangePosition: Decoder[RangePosition] =
      deriveDecoder[RangePosition]

    private implicit val decodeCompilationInfo: Decoder[CompilationInfo] =
      deriveDecoder[CompilationInfo]

    implicit val decodeEvalResponse: Decoder[EvalResponse] =
      deriveDecoder[EvalResponse]
  }
}

import EvalApi._

object BotApi {

  class BotOps[F[_]](implicit I: Inject[BotOp, F]) {
    import BotOp._

    object debug {

      def trace(msg: String): Free[F, Unit] = Free.inject(Debug.Tell(msg))
      def ask(prompt: String): Free[F, String] = Free.inject(Debug.Ask(prompt))
      def delay(duration: FiniteDuration): Free[F, Unit] = Free.inject(Debug.Delay(duration))
      def delay(ms: Long): Free[F, Unit] = delay(Duration(ms, MILLISECONDS))

    }
  }

  object BotOps {
    def freeIn[F[_]](implicit I: Inject[BotOp, F]): BotOps[F] = new BotOps[F]
  }

  sealed trait BotOp[A]
  object BotOp {

    object Debug {
      case class Ask(prompt: String) extends BotOp[String]
      case class Tell(msg: String) extends BotOp[Unit]
      case class Delay(duration: FiniteDuration) extends BotOp[Unit]
    }

    def defaultInterpreter[F[_]](implicit strategy: Strategy, F: Async[F]) =
      new (BotOp ~> F) {
        def apply[A](i: BotOp[A]) = i match {
          case Debug.Ask(prompt) ⇒
            F.delay {
              println(prompt)
              StdIn.readLine()
            }
          case Debug.Tell(msg)       ⇒ F.delay(println(msg))
          case Debug.Delay(duration) ⇒ F.delay(java.lang.Thread.sleep(duration.toMillis))
        }
      }

  }

  type Bot2[A] = EvalOp[A]
  type Bot1[A] = Coproduct[RtmOp, Bot2, A]
  type Bot0[A] = Coproduct[BotOp, Bot1, A]
  type Bot[A] = Coproduct[WebOp, Bot0, A]
  type IO[A] = Free[Bot, A]
}

case class BotApi() {
  import BotApi._

  val rtm = RtmOps.freeIn[Bot]
  val web = WebOps.freeIn[Bot]
  val bot = BotOps.freeIn[Bot]
  val eval = EvalOps.freeIn[Bot]
  val debug = bot.debug

}
