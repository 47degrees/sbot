/*
 * Scala Bot [common]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.common

import scala.inline
import scala.Console
import scala.NotImplementedError
import scala.Tuple2

object minidef {

  def ??? : Nothing = throw new NotImplementedError

  implicit final class ArrowAssoc[A](private val self: A) extends scala.AnyVal {
    @inline def → [B](y: B): Tuple2[A, B] = Tuple2(self, y)
  }

  def print(x: Any) = Console.print(x)
  def println(x: Any) = Console.println(x)

  @inline def identity[A](a: A): A = a
  def implicitly[A](implicit a: A): A = a

  type Any = scala.Any
  type AnyRef = scala.AnyRef
  type AnyVal = scala.AnyVal
  type Boolean = scala.Boolean
  type Byte = scala.Byte
  type Char = scala.Char
  type Double = scala.Double
  type Float = scala.Float
  type Int = scala.Int
  type Long = scala.Long
  type Nothing = scala.Nothing
  type PartialFunction[A, B] = scala.PartialFunction[A, B]
  type Product = scala.Product
  type Serializable = scala.Serializable
  type Short = scala.Short
  type String = java.lang.String
  type Unit = scala.Unit

  type Exception = java.lang.Exception

  final val Boolean = scala.Boolean
  final val Byte = scala.Byte
  final val Char = scala.Char
  final val Double = scala.Double
  final val Float = scala.Float
  final val Int = scala.Int
  final val Long = scala.Long
  final val Short = scala.Short
  final val Unit = scala.Unit

  // keeps interpolation happy
  final val StringContext = scala.StringContext

  object System {
    val err = java.lang.System.err
  }

  type tailrec = scala.annotation.tailrec

}
