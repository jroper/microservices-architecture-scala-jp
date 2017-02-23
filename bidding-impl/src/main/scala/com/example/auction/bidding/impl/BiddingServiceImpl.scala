package com.example.auction.bidding.impl

import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.example.auction.bidding.api
import com.example.auction.bidding.api.BiddingService
import com.example.auction.security.ServerSecurity._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class BiddingServiceImpl(persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends BiddingService {

  override def placeBid(itemId: UUID) = authenticated(userId => ServerServiceCall { bid =>
    entityRef(itemId).ask(PlaceBid(bid.maximumBidPrice, userId))
      .map { result =>
        val status = result.status match {
          case PlaceBidStatus.Accepted => api.BidResultStatus.Accepted
          case PlaceBidStatus.AcceptedBelowReserve => api.BidResultStatus.AcceptedBelowReserve
          case PlaceBidStatus.AcceptedOutbid => api.BidResultStatus.AcceptedOutbid
          case PlaceBidStatus.Cancelled => api.BidResultStatus.Cancelled
          case PlaceBidStatus.Finished => api.BidResultStatus.Finished
          case PlaceBidStatus.NotStarted => api.BidResultStatus.NotStarted
          case PlaceBidStatus.TooLow => api.BidResultStatus.TooLow
        }

        api.BidResult(result.currentPrice, status, result.currentBidder)
      }
  })

  override def getBids(itemId: UUID) = ServerServiceCall { _ =>
    entityRef(itemId).ask(GetAuction).map { auction =>
      auction.biddingHistory.map(convertBid).reverse
    }
  }

  override def startAuction = ServiceCall { auction =>
    entityRef(auction.itemId).ask(StartAuction(Auction(auction.itemId, auction.creator,
      auction.reservePrice, auction.increment, auction.startDate, auction.endDate))).map(_ => NotUsed)
  }

  override def bidEvents = TopicProducer.taggedStreamWithOffset(
    AuctionEvent.Tag.allTags.to[immutable.Seq]
  ) { (tag, offset) =>
    persistentEntityRegistry.eventStream(tag, offset)
      .collect {
        case EventStreamElement(itemId, BidPlaced(bid), offset) =>
          val message = api.BidPlaced(UUID.fromString(itemId), convertBid(bid))
          Future.successful((message, offset))
        case EventStreamElement(itemId, BiddingFinished, offset) =>
          persistentEntityRegistry.refFor[AuctionEntity](itemId)
            .ask(GetAuction).map { auction =>
              val message = api.BiddingFinished(UUID.fromString(itemId),
                auction.biddingHistory.headOption
                  .filter(_.bidPrice >= auction.auction.get.reservePrice)
                  .map(convertBid))

              (message, offset)
            }
      }.mapAsync(1)(identity)
  }

  private def convertBid(bid: Bid): api.Bid = api.Bid(bid.bidder, bid.bidTime, bid.bidPrice, bid.maximumBid)

  private def entityRef(itemId: UUID) = persistentEntityRegistry.refFor[AuctionEntity](itemId.toString)

}
