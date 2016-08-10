/*
 * Scala Bot [common]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.common
package http

import sbot.common.minidef._

import io.circe.Decoder
import io.circe.jawn

import fs2._
import fs2.util.Async

import cats.data.Xor

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class AkkaHttpPie(
    http: HttpExt,
    entityTimeout: FiniteDuration
)(implicit strategy: Strategy) extends HttpPie {

  private implicit val materializer = ActorMaterializer()(http.system)

  private val baseUnmarshaller = Unmarshaller
    .byteStringUnmarshaller
    .forContentTypes(`application/json`)

  override def post[A: Decoder](
    uri: String, query: (String, String)*
  ): Task[A] = Task.fromFuture {

    val req: Future[HttpResponse] = http.singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri    = uri,
      entity = FormData(query: _*).toEntity
    ))

    req.flatMap(
      resp ⇒ baseUnmarshaller
        .mapWithCharset((data, charset) ⇒
          jawn.decode(data.decodeString(charset.nioCharset.name))
            .valueOr(throw _))
        .apply(resp.entity))

  }

  private[this] def createStream[F[_]: Async, A](
    bind: (async.mutable.Queue[F, A]) ⇒ Unit
  ): Stream[F, A] =
    for {
      q ← Stream.eval(async.unboundedQueue[F, A])
      _ ← Stream.suspend {
        bind(q)
        Stream.emit(())
      }
      a ← q.dequeue
    } yield a

  def websocket[A: Decoder](
    uri: String
  ): Stream[Task, A] = createStream[Task, A]{ q ⇒

    val input = Flow[Message]
      .collect { case TextMessage.Strict(msg) ⇒ jawn.decode(msg) }
      .to(Sink.foreach[Any Xor A]{
        case Xor.Right(yay)  ⇒ q.enqueue1(yay).unsafeRunAsync(_ ⇒ ())
        case Xor.Left(error) ⇒ System.err.println("> " + error) // TODO: propagate?
      })

    val (upgradeResponse, closed) =
      http.singleWebSocketRequest(
        WebSocketRequest(uri),
        Flow.fromSinkAndSourceMat(
          input,
          Source.maybe)(Keep.both))
  }

}

object AkkaHttpPie {

  def apply(
    system: ActorSystem,
    entityTimeout: FiniteDuration,
    strategy: Strategy): HttpPie = AkkaHttpPie(
    Http(system), entityTimeout)(strategy)

  def apply(
    entityTimeout: FiniteDuration,
    strategy: Strategy): HttpPie = apply(
    ActorSystem("default", ConfigFactory.parseString("akka.daemonic=on")),
    entityTimeout,
    strategy
  )

}
