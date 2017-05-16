/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api
package data

sealed trait Supports {
  type RTMOnly[A]
}
object Supports {
  sealed trait RTM extends Supports
  sealed trait EventsAPI extends Supports
  sealed trait Web extends Supports
}
