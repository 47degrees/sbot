/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api
package web

import sbot.common.minidef._

import sbot.common.http.HttpPie

import cats.~>
import fs2.Task

class DefaultWebOpInterpreter(token: String, pie: HttpPie) extends (web.WebOp ~> Task) {
  def apply[A](op: web.WebOp[A]): Task[A] = {

    val resp = pie.post(op.url, ("token" → token) :: op.params: _*)(
      data.Response.decodeResponse(op.decoder)
    )

    resp flatMap {
      case data.Response.Success(payload, warnings) ⇒ Task.now(payload)
      case data.Response.Error(error)               ⇒ Task.fail(new Exception(error))
    }
  }
}
