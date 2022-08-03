package com.itforelead.workout.utils.derevo

import scala.annotation.implicitNotFound

import derevo.{ Derivation, NewTypeDerivation }

trait Derive[F[_]] extends Derivation[F] with NewTypeDerivation[F] {
  def instance(implicit ev: OnlyNewtypes): Nothing = ev.absurd

  @implicitNotFound("Only newtypes instances can be derived")
  final abstract class OnlyNewtypes {
    def absurd: Nothing = ???
  }
}
