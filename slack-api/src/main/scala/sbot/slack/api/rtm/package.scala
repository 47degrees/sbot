/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api

import sbot.common.minidef._
import sbot.common.http.HttpPie

import data.Event
import web.WebOp

import cats.Monoid
import cats.TransLift
import cats.arrow.{ NaturalTransformation ⇒ ~> }
import cats.free.Free
import cats.free.Inject
import cats.kernel.std.unit._

import fs2.Task
import fs2.Sink
import fs2.Stream
import fs2.async.mutable.Queue
import fs2.util.Async
import io.circe.Encoder
import io.circe.Json
import io.circe.JsonObject
import io.circe.ObjectEncoder
import io.circe.generic.semiauto._

package object rtm {

  class RTM(http: HttpPie) {
    def begin(url: String): RTM.Exchange =
      http.websocket[Event.RTM, Json](url)

    def begin(start: WebOp.RTM.Start.Resp): RTM.Exchange =
      begin(start.url)
  }

  object RTM {
    type Exchange = (Stream[Task, Event.RTM], Sink[Task, Json])
    def apply(http: HttpPie): RTM = new RTM(http)
  }

  //

  sealed abstract class RtmOp[A: Monoid] {
    final val a: A = Monoid[A].empty
  }

  object RtmOp {

    case class Typing(channel: data.ChannelId) extends RtmOp[Unit]

    private implicit val encodeTyping: ObjectEncoder[Typing] =
      deriveEncoder[Typing]

    implicit val encodeRtmOp: ObjectEncoder[RtmOp[_]] = new ObjectEncoder[RtmOp[_]] {

      def encode[A <: RtmOp[_]: ObjectEncoder](typeString: String): ObjectEncoder[A] =
        ObjectEncoder[A].mapJsonObject(
          _.add("type", Json.fromString(typeString)))

      final def encodeObject(op: RtmOp[_]): JsonObject = op match {
        case typing: Typing ⇒ encode[Typing]("typing").encodeObject(typing)
      }
    }

    def defaultInterpreter[F[_]](
      queue: Queue[F, Json]
    )(implicit F: Async[F]): RtmOp ~> F =
      new (RtmOp ~> F) {
        def apply[A](i: RtmOp[A]) =
          F.map(queue.offer1(Encoder[RtmOp[_]].apply(i)))(_ ⇒ i.a)
      }
  }

  //

  object RtmOps {

    def free: RtmOps[Free, RtmOp] = new RtmOps[Free, RtmOp](~>.id)

    def freeIn[F[_]: Inject[RtmOp, ?[_]]]: RtmOps[Free, F] =
      new RtmOps[Free, F](new (RtmOp ~> F) {
        def apply[A](op: RtmOp[A]): F[A] = Inject[RtmOp, F].inj(op)
      })

    def raw: RtmOps[λ[(α[_], β) ⇒ α[β]], RtmOp] =
      new RtmOps[λ[(α[_], β) ⇒ α[β]], RtmOp](~>.id)

  }

  class RtmOps[MT[_[_], _], F[_]] private[rtm] (f: RtmOp ~> F)(implicit ev: TransLift.AuxId[MT]) {
    type IO[A] = MT[F, A]
    private[this] def lift[A](op: RtmOp[A]): IO[A] = ev.liftT(f(op))

    def emitTyping(
      channel: data.ChannelId
    ): IO[Unit] = lift(RtmOp.Typing(channel))
  }

}
