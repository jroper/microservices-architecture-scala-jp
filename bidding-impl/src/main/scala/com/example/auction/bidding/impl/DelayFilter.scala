package com.example.auction.bidding.impl

import akka.actor.ActorSystem
import java.util.concurrent.TimeUnit
import play.api.Configuration
import play.api.mvc.{EssentialAction, EssentialFilter, Result}
import scala.concurrent.duration._
import scala.concurrent.Promise

/**
 * Inserts a delay to every request to simulate the bidding service being under heavy load
 */
class DelayFilter(val actorSystem: ActorSystem, val configuration: Configuration) extends EssentialFilter {

  private val responseDelay = configuration.underlying.getDuration("insert-delay", TimeUnit.MILLISECONDS).milliseconds
  private val enabled = responseDelay.toMillis > 0

  import actorSystem.dispatcher

  override def apply(next: EssentialAction) = {
    if (enabled) {
      EssentialAction { requestHeader =>
        next(requestHeader).mapFuture { result =>
          val delayedPromise = Promise[Result]()
          actorSystem.scheduler.scheduleOnce(responseDelay) {
            delayedPromise.success(result)
          }
          delayedPromise.future
        }
      }
    } else next
  }
}