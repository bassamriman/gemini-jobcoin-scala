package com.gemini.jobcoin.actors

import java.time.LocalDateTime

import akka.actor.{ActorRef, Props}
import com.gemini.jobcoin.common.MixerActor
import com.gemini.jobcoin.mixrequest.MixRequestTask

/**
  * ValidatorActor is responsible for validating that transactions are indeed stored
  * in the jobcoin ledger
  */
case class ValidatorActor() extends MixerActor {

  import ValidatorActor._

  override def receive: Receive = logged(handle(Map.empty, Map.empty))

  def handle(mixRequestTaskToValidate: Map[String, MixRequestTask],
             mixRequestTaskIdToSender: Map[String, ActorRef]): Receive = {
    case Validate(mixRequestTasks) =>
      val sender = context.sender()
      val newMixRequestTaskToValidate =
        mixRequestTaskToValidate ++ mixRequestTasks.map(
          mixRequestTask => mixRequestTask.id -> mixRequestTask
        )
      val newMixRequestTaskIdToSender: Map[String, ActorRef] =
        mixRequestTaskIdToSender ++ mixRequestTasks.map(
          mixRequestTask => mixRequestTask.id -> sender
        )
      context.become(
        logged(
          handle(
            mixRequestTaskToValidate = newMixRequestTaskToValidate,
            mixRequestTaskIdToSender = newMixRequestTaskIdToSender
          )
        )
      )

    case LedgerPublisherActor.LatestLedger(newLedger) =>
      //TODO: Make this more efficient. Loop through all transactions once for multiple mix request
      val presentMixRequestTaskIds: Seq[String] = mixRequestTaskToValidate
        .mapValues(
          mixRequestTask =>
            newLedger.exist(mixRequestTask.transaction.basicTransaction)
        )
        .filter(_._2)
        .keys
        .toSeq
      val presentMixRequestTasks: Seq[MixRequestTask] =
        presentMixRequestTaskIds.flatMap(
          presentMixRequestTaskId =>
            mixRequestTaskToValidate.get(presentMixRequestTaskId)
        )
      val newMixRequestTaskToValidate = mixRequestTaskToValidate -- presentMixRequestTaskIds
      val newMixRequestTaskIdToSender = mixRequestTaskIdToSender -- presentMixRequestTaskIds

      val groupedSenderToMixRequestTasks =
        presentMixRequestTasks.groupBy(
          presentMixRequestTask =>
            mixRequestTaskIdToSender(presentMixRequestTask.id)
        )

      groupedSenderToMixRequestTasks.foreach { groupedSenderToMixRequestTask =>
        val (sender, mixRequestTasks) = groupedSenderToMixRequestTask
        sender ! Validated(mixRequestTasks, LocalDateTime.now())
      }

      context.become(
        logged(
          handle(
            mixRequestTaskToValidate = newMixRequestTaskToValidate,
            mixRequestTaskIdToSender = newMixRequestTaskIdToSender
          )
        )
      )
  }

}

object ValidatorActor {
  def props: Props = Props(ValidatorActor())

  case class Validate(mixRequestTasks: Seq[MixRequestTask])

  case class Validated(mixRequestTasks: Seq[MixRequestTask],
                       timestamp: LocalDateTime)

}
