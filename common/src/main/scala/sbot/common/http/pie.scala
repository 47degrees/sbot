/*
 * Scala Bot [common]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.common
package http

import sbot.common.minidef._

import io.circe.Decoder
import fs2.Task
import fs2.Stream

trait HttpPie {

  def post[A: Decoder](
    uri: String, query: (String, String)*
  ): Task[A]

  def websocket[A: Decoder](uri: String): Stream[Task, A]
}
