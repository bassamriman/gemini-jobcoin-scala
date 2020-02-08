package com.gemini.jobcoin.actors

import akka.actor.{ActorRef, Props}
import com.gemini.jobcoin.accounting.BasicLedger
import com.gemini.jobcoin.common.MixerActor

case class LedgerActor(subscribers: Seq[ActorRef], apiAccessActor: ActorRef)
    extends MixerActor {

  import LedgerActor._

  override def receive: Receive = logged(handle)

  def handle: Receive = {
    case FetchLatestLedger => apiAccessActor ! APIAccessActor.GetAllTransactions
    case APIAccessActor.AllTransactionsLedger(newLedger) =>
      subscribers.foreach(_ ! LatestLedger(newLedger))
  }
}

object LedgerActor {
  def props(subscribers: Seq[ActorRef], apiAccessActor: ActorRef): Props =
    Props(LedgerActor(subscribers, apiAccessActor))

  case object SendMeLatestLedger

  case object FetchLatestLedger

  case class LatestLedger(ledger: BasicLedger)

}
