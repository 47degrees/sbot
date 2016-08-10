/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api

import sbot.common.minidef._
import sbot.common.http.HttpPie
import data.Event
import web.WebOp
import fs2.Task
import fs2.Stream

package object rtm {

  class RTM(http: HttpPie) {
    def begin(url: String): Stream[Task, Event.RTM] =
      http.websocket[Event.RTM](url)

    def begin(start: WebOp.RTM.Start.Resp): Stream[Task, Event.RTM] =
      begin(start.url)
  }

  object RTM {
    def apply(http: HttpPie): RTM = new RTM(http)
  }

}
