/*
 * Scala Bot [common]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.common
package http

import sbot.common.minidef._

import scala.collection.immutable.List
import scala.collection.immutable.Nil

import io.circe.Decoder
import io.circe.Encoder
import fs2.Task
import fs2.Sink
import fs2.Stream

object HttpPie {
  type Param = (String, String)
  type Header = (String, String)
}

trait HttpPie {
  import HttpPie._

  def post[A: Decoder](uri: String, query: Param*): Task[A]

  def post[Q: Encoder, A: Decoder](
    uri: String, query: Q, headers: List[Header] = Nil): Task[A]

  def websocket[I: Decoder, O: Encoder](uri: String): (Stream[Task, I], Sink[Task, O])
}
