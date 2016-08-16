/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack.api.util

import sbot.common.minidef._

import scala.collection.immutable.List
import scala.Predef.augmentString

// This really needs testing!
object SlackQuoteCleaner {

  def clean(input: String): String =
    parseRegular(input, input.length, 0, 0, List.empty).reverse.mkString("")

  private[this] def isDoubleQuote(char: Char): Boolean = char match {
    case '“' | '”' | '"' ⇒ true
    case _               ⇒ false
  }

  private[this] def countDoubleQuotes(input: String, offset: Int, max: Int): Int =
    if (!isDoubleQuote(input(offset))) 0
    else if (max - offset >= 2 &&
      isDoubleQuote(input(offset + 1)) &&
      isDoubleQuote(input(offset + 2))) 3
    else 1

  private[this] def parseInQuotes(input: String, max: Int,
    nQuotes: Int,
    offset0: Int, offset: Int, acc: List[String]): List[String] = if (offset >= max)
    input.substring(offset0, max) :: acc
  else {
    val c = input(offset)
    if (c == '\\' && max - offset >= 1) {
      val c2 = input(offset + 1)
      val delta = if (c2 == '\\' || isDoubleQuote(c2)) 2
      else 1
      parseInQuotes(input, max, nQuotes, offset0, offset + delta, acc)
    } else {

      val nQuotes2 = countDoubleQuotes(input, offset, max)
      if (nQuotes2 == nQuotes)
        parseRegular(input, max, offset + nQuotes, offset + nQuotes,
          "\"" * nQuotes ::
            input.substring(offset0, offset) :: acc)
      else
        parseInQuotes(input, max, nQuotes, offset0, offset + 1, acc)
    }
  }

  private[this] def parseRegular(
    input: String,
    max: Int,
    offset0: Int, offset: Int,
    acc: List[String]
  ): List[String] = if (offset >= max) input.substring(offset0, max) :: acc
  else {
    val nQuotes = countDoubleQuotes(input, offset, max)
    if (nQuotes > 0) parseInQuotes(input, max, nQuotes, offset + nQuotes, offset + nQuotes,
      "\"" * nQuotes ::
        input.substring(offset0, offset) :: acc)
    else
      parseRegular(input, max, offset0, offset + 1, acc)
  }

}
