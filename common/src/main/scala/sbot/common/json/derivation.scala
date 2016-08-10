/*
 * Scala Bot [common]
 *  … yet another 🤖 framework, for Scala
 */

package sbot
package common.json

import io.circe.Decoder
import io.circe.Json
import io.circe.generic.decoding.DerivedDecoder
import shapeless.{ HList, LabelledGeneric, Lazy }
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

/** Json derivation helpers!
  *
  * This is a thin layer on top of the semi auto derivation provided
  * by Circe.
  */
object derivation {

  // poor man solution for currying on type A, so that
  // we don't have to provide A, P, C, T, R... (see below)
  // and we can also 'overload'
  final class MakeDecoder[A] private[derivation]

  object MakeDecoder extends MakeDecoderImplicitsLowPrio {
    implicit def toDecoderWithRaw[A, P <: HList, C, T <: HList, R <: HList](
      h: MakeDecoder[A]
    )(implicit
      ffp: FnFromProduct.Aux[P ⇒ C, Json ⇒ A],
      gen: LabelledGeneric.Aux[C, T],
      removeAll: RemoveAll.Aux[T, P, (P, R)],
      decode: DerivedDecoder[R]): Decoder[A] =
      Decoder.instance(c ⇒
        DerivedDecoder.decodeIncompleteCaseClass[Json ⇒ A, P, C, T, R]
          .apply(c).map(_(c.focus)))
  }

  sealed trait MakeDecoderImplicitsLowPrio {
    implicit def toDecoder[A](h: MakeDecoder[A])(
      implicit
      decode: Lazy[DerivedDecoder[A]]): Decoder[A] = decode.value
  }

  def makeDecoder[A] = new MakeDecoder[A]

}
