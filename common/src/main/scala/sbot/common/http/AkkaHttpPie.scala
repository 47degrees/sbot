/*
 * Scala Bot [common]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.common
package http

import sbot.common.minidef._

import io.circe.Decoder
import io.circe.Encoder
import io.circe.jawn

import fs2._

import cats.data.Xor

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import com.typesafe.config.ConfigFactory

import scala.collection.immutable.List
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
  //.forContentTypes(`application/json`)

  override def post[A: Decoder](
    uri: String, query: (String, String)*
  ): Task[A] = makePostRequest(
    uri     = uri,
    entity  = FormData(query: _*).toEntity,
    headers = List.empty
  )

  override def post[Q: Encoder, A: Decoder](
    uri: String, query: Q, headers: List[(String, String)]
  ): Task[A] = makePostRequest(
    uri     = uri,
    entity  = HttpEntity(
      ContentTypes.`application/json`, Encoder[Q].apply(query).noSpaces),
    headers = headers
  )

  private[this] def makePostRequest[A: Decoder](
    uri: String,
    entity: RequestEntity,
    headers: List[(String, String)]
  ): Task[A] = Task.fromFuture {

    val req: Future[HttpResponse] = http.singleRequest(HttpRequest(
      method  = HttpMethods.POST,
      uri     = uri,
      entity  = entity,
      headers = headers.map(kv ⇒ RawHeader(kv._1, kv._2))
    ))

    req.flatMap(
      resp ⇒ baseUnmarshaller
        .mapWithCharset((data, charset) ⇒ {
          val dataString = data.decodeString(charset.nioCharset.name)
          jawn.decode(dataString)
            .valueOr(throw _)
        })
        .apply(resp.entity))

  }

  def websocket[I: Decoder, O: Encoder](
    uri: String
  ): (Stream[Task, I], fs2.Sink[Task, O]) = {

    val akkaInputSink = Sink.queue[I]()
    val akkaInput = Flow[Message]
      .collect { case TextMessage.Strict(msg) ⇒ jawn.decode(msg) }
      .collect { case Xor.Right(value) ⇒ value }
      .toMat(akkaInputSink)(Keep.right)

    val akkaOutput = Source.queue[Message](100, OverflowStrategy.backpressure)

    val (upgradeResponse, (akkaInputQueue, akkaOutputQueue)) =
      http.singleWebSocketRequest(
        WebSocketRequest(uri),
        Flow.fromSinkAndSourceMat(
          akkaInput,
          akkaOutput)(Keep.both))

    val outputSink: fs2.Sink[Task, O] =
      s ⇒ s.map { o ⇒
        val text: String = Encoder[O].apply(o).noSpaces
        val message: Message = TextMessage(text)
        akkaOutputQueue offer message
        ()
      }

    val inputStream = Stream
      .repeatEval(Task.fromFuture(akkaInputQueue.pull))
      .through(pipe.unNoneTerminate)

    inputStream → outputSink

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
