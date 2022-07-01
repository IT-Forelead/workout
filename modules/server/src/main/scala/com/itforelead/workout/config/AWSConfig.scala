package com.itforelead.workout.config

import com.itforelead.workout.domain.custom.refinements.{BucketName, UrlAddress}
import eu.timepit.refined.types.string.NonEmptyString

case class AWSConfig(
  accessKey: NonEmptyString,
  secretKey: NonEmptyString,
  serviceEndpoint: UrlAddress,
  signingRegion: NonEmptyString,
  bucketName: BucketName
)
