/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api

import sbot.common.minidef._

import scala.Option
import scala.None
import scala.collection.immutable.List
import scala.collection.immutable.{ List ⇒ List_ }

import cats.Monad
import cats.TransLift
import cats.arrow.{ NaturalTransformation ⇒ ~> }
import cats.data.Kleisli
import cats.free.Free
import cats.free.Inject
import cats.syntax.option._

import io.circe.Decoder
import io.circe.Json
import io.circe.generic.semiauto._

import fs2.Task
import scala.concurrent.Future

package object web {

  sealed abstract class WebOp[A: Decoder](val url: String, _params: Param*) {
    implicit val decoder = Decoder[A]
    def params: List[(String, String)] = _params.toList.flatten
  }

  object WebOp {

    // these are organized/named according to the Slack API documentation

    object Channels {

      case class Archive(
        channel: data.ChannelId
      ) extends WebOp[Unit](
        "https://slack.com/api/channels.archive",
        "channel" → channel
      )

      case class Create(
        name: String
      ) extends WebOp[data.Channel](
        "https://slack.com/api/channels.create",
        "name" → name
      )(downField("channel"))

      case class Join(
        name: String
      ) extends WebOp[data.Channel](
        "https://slack.com/api/channels.join",
        "name" → name
      )(downField("channel"))

      case class History(
        channel: data.ChannelId,
        latest: Option[String],
        oldest: Option[String],
        inclusive: Option[Boolean], // int style,
        count: Option[Int],
        unreads: Option[Boolean] // int style
      ) extends WebOp[History.Resp](
        "https://slack.com/api/channels.history",
        "channel" → channel,
        "latest" → latest,
        "oldest" → oldest,
        "inclusive" → inclusive,
        "count" → count,
        "unreads" → unreads
      )

      object History {
        case class Resp(
          messages: List_[data.Message.Web],
          hasMore: Boolean
        )
        private implicit val decodeResp: Decoder[Resp] =
          Decoder.forProduct2("messages", "has_more")(History.Resp.apply _)
      }

      case class Info(
        channel: data.ChannelId
      ) extends WebOp[data.Channel](
        "https://slack.com/api/channels.info",
        "channel" → channel
      )(downField("channel"))

      case class List(
        excludeArchived: Boolean
      ) extends WebOp[List_[data.Channel]](
        "https://slack.com/api/channels.list",
        "exclude_archived" → excludeArchived
      )(downField("channels"))
    }

    object Chat {

      case class PostMessage(
        channel: data.ChannelId,
        text: Option[String],
        linkNames: Option[Boolean],
        asUser: Option[Boolean]
      ) extends WebOp[PostMessage.Resp](
        "https://slack.com/api/chat.postMessage",
        "channel" → channel,
        "text" → text,
        "link_names" → linkNames,
        "as_user" → asUser
      )

      object PostMessage {
        case class Resp(
          channel: data.ChannelId,
          ts: data.TS,
          message: data.Message.Web
        )
        private implicit val decodeResp: Decoder[Resp] =
          deriveDecoder[Resp]
      }

    }

    object RTM {
      case class Start(
        simpleLatest: Option[Boolean],
        noUnreads: Option[Boolean],
        mpimAware: Option[Boolean]
      ) extends WebOp[Start.Resp](
        "https://slack.com/api/rtm.start",
        "simple_latest" → simpleLatest,
        "no_unreads" → noUnreads,
        "mpim_aware" → mpimAware
      )

      object Start {
        case class Resp(
          url: String,
          self: data.User // TODO??
        )
        private implicit val decodeResp: Decoder[Resp] =
          deriveDecoder[Resp]
      }

    }

  }

  object WebOps {

    /** The WebOps API operating in `fs2.Task` */
    def task(interpreter: WebOp ~> Task): WebOps[λ[(α[_], β) ⇒ α[β]], Task] =
      new WebOps[λ[(α[_], β) ⇒ α[β]], Task](interpreter)

    /** The WebOps API operating in Scala's `Future` */
    def future(interpreter: WebOp ~> Task): WebOps[λ[(α[_], β) ⇒ α[β]], Future] =
      new WebOps[λ[(α[_], β) ⇒ α[β]], Future](interpreter andThen new (Task ~> Future) {
        def apply[A](task: Task[A]): Future[A] = task.unsafeRunAsyncFuture()
      })

    /** The WebOps API operating for the simple `Free` algebra */
    def free: WebOps[Free, WebOp] = new WebOps[Free, WebOp](~>.id)

    /** The WebOps API injected into a higher Free algebra */
    def freeIn[F[_]: Inject[WebOp, ?[_]]]: WebOps[Free, F] =
      new WebOps[Free, F](new (WebOp ~> F) {
        def apply[A](op: WebOp[A]): F[A] = Inject[WebOp, F].inj(op)
      })

    /** The WebOps API for returning the raw ADT */
    def raw: WebOps[λ[(α[_], β) ⇒ α[β]], WebOp] =
      new WebOps[λ[(α[_], β) ⇒ α[β]], WebOp](~>.id)

  }

  /** The WebOps API */
  class WebOps[MT[_[_], _], F[_]] private[web] (f: WebOp ~> F)(implicit ev: TransLift.AuxId[MT]) {
    type IO[A] = MT[F, A]
    private[this] def lift[A](op: WebOp[A]): IO[A] = ev.liftT(f(op))

    object channels {
      def list(
        excludeArchived: Boolean = false
      ): IO[List[data.Channel]] = lift(
        WebOp.Channels.List(excludeArchived))
    }

    object chat {
      def postMessage(
        channel: data.ChannelId,
        text: String,
        linkNames: Boolean = true,
        asUser: Boolean = true
      ): IO[WebOp.Chat.PostMessage.Resp] = lift(
        WebOp.Chat.PostMessage(
          channel, text.some, linkNames.some, asUser.some))
    }

    object rtm {
      def start(
        simpleLatest: Option[Boolean] = None,
        noUnreads: Option[Boolean] = None,
        mpimAware: Option[Boolean] = None
      ): IO[WebOp.RTM.Start.Resp] = lift(
        WebOp.RTM.Start(simpleLatest, noUnreads, mpimAware))
    }

  }

  implicit class FreeWebOpOps[A](ma: Free[WebOp, A]) {
    def invoke[M[_]: Monad](xop: WebOp ~> M): M[A] =
      ma.foldMap(xop)
  }

  // --
  // - helpers & parameter DSL
  // --

  private def downField[A: Decoder](path: String): Decoder[A] =
    Decoder.instance(_.downField(path).as[A])

  private type Param = Option[(String, String)]
  private implicit def paramFromTuple2[A](kv: (String, A))(implicit ev: Render[A]): Param =
    ev(kv._2).map(kv._1 → _)

  private type Render[A] = Kleisli[Option, A, String]

  private implicit val renderInt: Render[Int] = Kleisli(v ⇒ v.toString.some)
  private implicit val renderBoolean: Render[Boolean] = Kleisli(v ⇒ v.toString.some)
  private implicit val renderString: Render[String] = Kleisli(_.some)

  private implicit def renderOption[A](implicit ev: Render[A]): Render[Option[A]] =
    Kleisli(_.flatMap(ev.run))
}
