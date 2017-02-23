package com.example.auction.item.impl

import java.util.UUID

import akka.Done
import akka.stream.scaladsl.Flow
import com.example.auction.bidding.api.{BidEvent, BidPlaced, BiddingFinished, BiddingService}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.Future

class BiddingServiceSubscriber(persistentEntityRegistry: PersistentEntityRegistry, biddingService: BiddingService) {



  private def entityRef(itemId: UUID) = persistentEntityRegistry.refFor[ItemEntity](itemId.toString)

}
