package com.example.auction.bidding.api

import java.time.Instant
import java.util.UUID

import play.api.libs.json.{Format, Json}

case class Auction(itemId: UUID, creator: UUID, reservePrice: Int, increment: Int, startDate: Instant, endDate: Instant)

object Auction {
  implicit val format: Format[Auction] = Json.format
}
