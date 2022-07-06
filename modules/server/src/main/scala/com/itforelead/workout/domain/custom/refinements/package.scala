package com.itforelead.workout.domain.custom

import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.boolean.{And, Not}
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{Interval, Negative}
import eu.timepit.refined.string.{MatchesRegex, Uri, Url}

package object refinements {
  private type EmailPred          = MatchesRegex["^[a-zA-Z0-9.-_]+@[a-zA-Z0-9]+\\.[a-zA-Z]+$"]
  private type PasswordPred       = MatchesRegex["^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[A-Z])[a-zA-Z0-9!@#$%^&*]{6,32}$"]
  private type FileNamePred       = MatchesRegex["^[\\w,\\s-]+\\.[A-Za-z0-z-]{1,}$"]
  private type TelNumberPred      = MatchesRegex["^[+][0-9]{12}$"]
  private type ValidationCodePred = MatchesRegex["^[0-9]{4}$"]
  private type BucketPred         = MatchesRegex["^[a-z0-9/.-]{3,63}$"]
  private type FilePathPred       = MatchesRegex["[a-z0-9-]+\\.+(png|jpg|jpeg|bmp|webp)"]
  private type FileKeyPred        = MatchesRegex["[a-z0-9-]+\\.+(png|jpg|jpeg|bmp|webp)"]
  private type ProductRatingPred  = Interval.Closed[1, 5] And Not[Negative]

  type EmailAddress = String Refined EmailPred
  object EmailAddress extends RefinedTypeOps[EmailAddress, String]

  type Password = String Refined (NonEmpty And PasswordPred)
  object Password extends RefinedTypeOps[Password, String]

  type UriAddress = String Refined Uri
  object UriAddress extends RefinedTypeOps[UriAddress, String]

  type FileName = String Refined FileNamePred
  object FileName extends RefinedTypeOps[FileName, String]

  type Tel = String Refined TelNumberPred
  object Tel extends RefinedTypeOps[Tel, String]

  type ValidationCode = String Refined (NonEmpty And ValidationCodePred)
  object ValidationCode extends RefinedTypeOps[ValidationCode, String]

  type BucketName = String Refined BucketPred
  object BucketType extends RefinedTypeOps[BucketName, String]

  type Prefix = String Refined NonEmpty
  object Prefix extends RefinedTypeOps[Prefix, String]

  type UrlAddress = String Refined Url
  object UrlAddress extends RefinedTypeOps[UrlAddress, String]

  type Key = String Refined NonEmpty
  object Key extends RefinedTypeOps[Key, String]

  type ProductRating = Double Refined ProductRatingPred
  object ProductRating extends RefinedTypeOps[ProductRating, Double]

  type FilePath = String Refined (NonEmpty And FilePathPred)
  object FilePath extends RefinedTypeOps[FilePath, String]

  type FileKey = String Refined (NonEmpty And FileKeyPred)
  object FileKey extends RefinedTypeOps[FileKey, String]

}
