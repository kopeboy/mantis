package io.iohk.ethereum.snappy

import io.iohk.ethereum.domain.{ Block, Receipt }
import io.iohk.ethereum.utils.Logger
import org.scalatest.{ FreeSpec, Matchers }

import scala.concurrent.duration._

class SnappyTest extends FreeSpec with Matchers with Logger {

  val config = Config()
  val pre = new Prerequisites(config)
  import pre._

  "Blockchain regression test" in {

    val startN = targetBlockchain match {
      case Some(tb) => config.startBlock.getOrElse(tb.getBestBlockNumber() - 1).max(1)
      case None => BigInt(1)
    }
    val targetN = config.targetBlock.getOrElse(sourceBlockchain.getBestBlockNumber()).max(1)

    val progLog = new ProgressLogger(startN, targetN, 5.seconds)

    progLog.start()

    for (n <- startN to targetN) {
      val block: Block = sourceBlockchain.getBlockByNumber(n)
        .getOrElse(fail(s"Failed to retrieve block by number: $n"))

      val expectedReceipts = sourceBlockchain.getReceiptsByHash(block.header.hash)
        .getOrElse(fail(s"Failed to retrieve receipts for block number: $n"))

      val result = executeBlock(block)

      result match {
        case Left(error) =>
          fail(s"Failed to execute block $n: $error")

        case Right(receipts) =>
          if (receipts == expectedReceipts) {
            targetBlockchain.foreach(_.save(block))
            targetBlockchain.foreach(_.save(block.header.hash, receipts))
          } else {
            fail(s"Block $n did not execute correctly.\n$receipts did not equal $expectedReceipts")
          }
      }

      progLog.update(n)
    }
  }

  private def executeBlock(block: Block): Either[Any, Seq[Receipt]] =
    targetBlockchain match {
      case Some(_) =>
        ledger.executeBlock(block)

      case None =>
        // this seems to discard failures, for better errors messages we might want to implement a different method (simulateBlock?)
        val result = blockPreparator.prepareBlock(block)
        Right(result.blockResult.receipts)
    }
}
