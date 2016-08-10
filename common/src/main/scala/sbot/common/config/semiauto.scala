/*
 * Scala Bot [common]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.common
package config

import minidef._

import scala.collection.immutable.Map
import scala.Some
import scala.Symbol

import shapeless._
import shapeless.labelled.{ field, FieldType }

import cats._
import cats.data._
import cats.implicits._

/** Semi-automatic derivation of case class "readers" for configuration data.
  *
  * @author Andy Scott [47 Degrees]
  */
object semiauto
    extends MapReadValueInstances
    with KnobsReadValueInstances { // format: OFF

  private type Res[A] = ValidatedNel[DecodeError, A]

  /** Decoder
    * A very thin wrapper around `C ⇒ Res[A]`.
    */
  case class Decoder[C, A](run: C ⇒ Res[A]) extends (C ⇒ Res[A]) {
    def apply(source: C): Res[A] = run(source)
  }
  object Decoder {
    def apply[C, A](implicit ev: Decoder[C, A]): Decoder[C, A] = ev
  }

  /** ReadValue
    * A very thin wrapper around `(C, String) ⇒ Res[A]`.
    */
  case class ReadValue[C, A](
    run: (C, String) ⇒ Res[A]
  ) extends ((C, String) ⇒ Res[A]) {
    def apply(config: C, key: String): Res[A] = run(config, key)
  }
  object ReadValue {
    def apply[C, A](implicit ev: ReadValue[C, A]): ReadValue[C, A] = ev
  }

  sealed trait DecodeError extends Product with Serializable
  object DecodeError {
    case class MissingKey(key: String) extends DecodeError
    case class WrongType(key: String) extends DecodeError
    case class AtPath(key: String, errors: NonEmptyList[DecodeError])
        extends DecodeError
    case class BadFormat(key: String, message: String) extends DecodeError
  }


  /** Derive a decoder for type `A` from source data of type `C`.
    */
  def deriveDecoder[C, A: LabelledGeneric: Decoder[C, ?]]: Decoder[C, A] =
    Decoder[C, A]

  implicit def deriveDecoder0[C, A, L <: HList](
    implicit gen: LabelledGeneric.Aux[A, L], readFields: Lazy[Decoder[C, L]]
  ): Decoder[C, A] = Decoder(config ⇒
    Kleisli(readFields.value.run).map(gen.from).apply(config))

  // backend
  // Note: yyz prefix chosen arbitrarily to avoid name conflicts
  // ... it's also an arbitrary reference to the Rush song

  implicit def yyzReadHNil[C]: Decoder[C, HNil] =
    Decoder(_ ⇒ Validated.valid(HNil))

  implicit def yyzReadHCons[
    C, K <: Symbol, H: ReadValue[C, ?], T <: HList: Decoder[C, ?]
  ](implicit key: Witness.Aux[K]): Decoder[C, FieldType[K, H] :: T] =
    Decoder(config ⇒
      Apply[Res].map2(
        ReadValue[C, H].apply(config, key.value.name),
        Decoder[C, T].apply(config)
      )((head, tail) ⇒ field[K](head) :: tail))
}

// format: ON

sealed trait MapReadValueInstances extends ReadValueInstances { self: semiauto.type ⇒
  import DecodeError._

  implicit def yyzReadMapSupport[A: Typeable]: ReadValue[Map[String, Any], A] =
    ReadValue((map, key) ⇒ {
      val typeCase = TypeCase[A]
      map.get(key) match {
        case Some(typeCase(value)) ⇒ Validated.valid(value)
        case Some(other)           ⇒ Validated.invalidNel(WrongType(key))
        case _                     ⇒ Validated.invalidNel(MissingKey(key))
      }
    })
}

sealed trait KnobsReadValueInstances extends ReadValueInstances { self: semiauto.type ⇒
  import DecodeError._
  import knobs._

  def deriveKnobsDecoder[A: Decoder[Config, ?]] = Decoder[Config, A]

  implicit def yyzReadKnobsSupport[A: Configured]: ReadValue[Config, A] =
    ReadValue((config, key) ⇒
      Xor.fromOption(config.env.get(key), MissingKey(key))
        .flatMap(value ⇒ Xor.fromOption(value.convertTo[A], WrongType(key)))
        .toValidatedNel)

  implicit def yyzKnobsNestedReadSupport[A: Decoder[Config, ?]]: ReadValue[Config, A] =
    ReadValue((config, key) ⇒
      Decoder[Config, A].apply(config.subconfig(key))
        .leftMap(errors ⇒ NonEmptyList(AtPath(key, errors))))

}

sealed trait ReadValueInstances { self: semiauto.type ⇒
  import scala.concurrent.duration._
  import DecodeError._

  implicit def yyzReadFiniteDuration[C](implicit ev: ReadValue[C, Duration]): ReadValue[C, FiniteDuration] =
    ReadValue((source, key) ⇒
      ev.apply(source, key) andThen {
        case fd: FiniteDuration ⇒ Validated.valid(fd)
        case d ⇒ Validated.invalidNel(
          BadFormat(key, s"$key is not a finite duration"))
      }
    )

  implicit def yyzReadDuration[C: ReadValue[?, String]]: ReadValue[C, Duration] =
    ReadValue((source, key) ⇒
      ReadValue[C, String].apply(source, key) andThen { value ⇒
        Xor.catchNonFatal(Duration(value))
          .leftMap(e ⇒ BadFormat(key, e.getMessage))
          .toValidatedNel
      }
    )
}
