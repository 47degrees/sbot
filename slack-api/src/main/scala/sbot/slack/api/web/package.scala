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

import cats._
import cats.data.Kleisli
import cats.free.Free
import cats.free.Inject
import cats.syntax.option._

import io.circe.Decoder
import io.circe.generic.semiauto._

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
          self: data.User,
          users: List[data.User],
          channels: List[data.Channel]
        )
        private implicit val decodeResp: Decoder[Resp] =
          deriveDecoder[Resp]
      }

    }

  }

  object WebOps {

    /** The WebOps API injected into a free algebra */
    def free[F[_]: Inject[WebOp, ?[_]]]: WebOps[Free[F, ?]] =
      WebOps[Free[F, ?]](λ[WebOp ~> Free[F, ?]](Free.inject(_)))
  }

  /** The WebOps API */
  case class WebOps[F[_]](eval: WebOp ~> F) {
    import WebOp._

    object channels {
      def list(
        excludeArchived: Boolean = false
      ): F[List[data.Channel]] = eval(
        Channels.List(excludeArchived))
    }

    object chat {
      def postMessage(
        channel: data.ChannelId,
        text: String,
        linkNames: Boolean = true,
        asUser: Boolean = true
      ): F[WebOp.Chat.PostMessage.Resp] = eval(
        Chat.PostMessage(
          channel, text.some, linkNames.some, asUser.some))
    }

    object rtm {
      def start(
        simpleLatest: Option[Boolean] = None,
        noUnreads: Option[Boolean] = None,
        mpimAware: Option[Boolean] = None
      ): F[WebOp.RTM.Start.Resp] = eval(
        RTM.Start(simpleLatest, noUnreads, mpimAware))
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
