package com.gemini.jobcoin.mixrequest

trait MixRequestCoordinate {
  def sourceAddress: String
  def destinationAddresses: Seq[String]
}

case class MixRequestCoordinateImpl(sourceAddress: String,
                                    destinationAddresses: Seq[String])
    extends MixRequestCoordinate

object MixRequestCoordinate {
  def apply(sourceAddress: String,
            destinationAddresses: Seq[String]): MixRequestCoordinate =
    MixRequestCoordinateImpl(sourceAddress, destinationAddresses)
}

trait MixRequestCoordinateDelegate {
  def mixRequestCoordinate: MixRequestCoordinate
  def sourceAddress: String = mixRequestCoordinate.sourceAddress
  def destinationAddresses: Seq[String] =
    mixRequestCoordinate.destinationAddresses
}
