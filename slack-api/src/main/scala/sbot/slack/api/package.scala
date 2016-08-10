/*
 * Scala Bot [slack-api]
 *  … yet another 🤖 framework, for Scala
 */

package sbot.slack

import cats.TransLift
import cats.Trivial

import cats.free.Free

package object api extends api.MiscTransLiftInstances

package api {
  private[api] sealed trait MiscTransLiftInstances {
    implicit val freeTransLift: TransLift.Aux[Free, Trivial.PH1] =
      new TransLift[Free] {
        type TC[M[_]] = Trivial.PH1[M]
        override def liftT[M[_]: TC, A](ma: M[A]) = Free.liftF(ma)
      }

    implicit val identityTransLift: TransLift.AuxId[λ[(α[_], β) ⇒ α[β]]] =
      new TransLift[λ[(α[_], β) ⇒ α[β]]] {
        type TC[M[_]] = Trivial.PH1[M]
        override def liftT[M[_]: TC, A](ma: M[A]): M[A] = ma
      }
  }
}
