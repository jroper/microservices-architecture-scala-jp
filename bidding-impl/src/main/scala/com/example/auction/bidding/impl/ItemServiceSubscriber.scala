package com.example.auction.bidding.impl

import java.util.UUID

import akka.Done
import akka.stream.scaladsl.Flow
import com.example.auction.item.api.{ItemEvent, ItemService}
import com.example.auction.item.api
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.Future

class ItemServiceSubscriber(persistentEntityRegistry: PersistentEntityRegistry, itemService: ItemService) {



  private def entityRef(itemId: UUID) = persistentEntityRegistry.refFor[AuctionEntity](itemId.toString)

}
