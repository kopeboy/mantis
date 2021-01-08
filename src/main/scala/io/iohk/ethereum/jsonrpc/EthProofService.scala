package io.iohk.ethereum.jsonrpc

import akka.util.ByteString
import cats.implicits._
import io.iohk.ethereum.consensus.blocks.BlockGenerator
import io.iohk.ethereum.domain.{Account, Address, Block, Blockchain, UInt256}
import io.iohk.ethereum.jsonrpc.EthService._
import io.iohk.ethereum.mpt.{MptNode, MptTraversals}
import monix.eval.Task

/** The key used to get the storage slot in its account tree */
final case class StorageProofKey(v: BigInt) extends AnyVal

/**
  * Object proving a relationship of a storage value to an account's storageHash
  *
  * @param key storage proof key
  * @param value the value of the storage slot in its account tree
  * @param proof the set of node values needed to traverse a patricia merkle tree (from root to leaf) to retrieve a value
  */
final case class StorageProof(
    key: StorageProofKey,
    value: BigInt,
    proof: Seq[ByteString]
)

/**
  * The merkle proofs of the specified account connecting them to the blockhash of the block specified.
  *
  * Proof of account consists of:
  * - account object: nonce, balance, storageHash, codeHash
  * - Markle Proof for the account starting with stateRoot from specified block
  * - Markle Proof for each requested storage entory starting with a storage Hash from the account
  *
  * @param address the address of the account or contract of the request
  * @param accountProof Markle Proof for the account starting with stateRoot from specified block
  * @param balance the Ether balance of the account or contract of the request
  * @param codeHash the code hash of the contract of the request (keccak(NULL) if external account)
  * @param nonce the transaction count of the account or contract of the request
  * @param storageHash the storage hash of the contract of the request (keccak(rlp(NULL)) if external account)
  * @param storageProof current block header PoW hash
  */
final case class ProofAccount(
    address: Address,
    accountProof: Seq[ByteString],
    balance: BigInt,
    codeHash: ByteString,
    nonce: UInt256,
    storageHash: ByteString,
    storageProof: Seq[StorageProof]
)

sealed trait AccountProofError
case object InvalidAccountProofOrRootHash extends AccountProofError
case object InvalidAccountProofForAccount extends AccountProofError

/**
  * Spec: [EIP-1186](https://eips.ethereum.org/EIPS/eip-1186)
  * besu: https://github.com/PegaSysEng/pantheon/pull/1824/files
  * parity: https://github.com/openethereum/parity-ethereum/pull/9001
  * geth: https://github.com/ethereum/go-ethereum/pull/17737
  */
class EthProofService(blockchain: Blockchain, blockGenerator: BlockGenerator, ethCompatibleStorage: Boolean) {

  /**
    * Get account and storage values for account including Merkle Proof.
    *
    * @param address address of the account
    * @param storageKeys storage keys which should be proofed and included
    * @param blockNumber block number or string "latest", "earliest"
    * @return
    */
  def run(
      address: Address,
      storageKeys: Seq[StorageProofKey],
      blockNumber: BlockParam
  ): Task[Either[JsonRpcError, ProofAccount]] = Task {
    for {
      blockNumber <- resolveBlock(blockNumber).map(_.block.number)
      account <- Either.fromOption(
        blockchain.getAccount(address, blockNumber),
        noAccount(address, blockNumber)
      )
      accountProof <- Either.fromOption(
        blockchain.getAccountProof(address, blockNumber).map(_.map(asRlpSerializedNode)),
        noAccountProof(address, blockNumber)
      )
      storageProof <- getStorageProof(account, storageKeys)
    } yield mkAccountProof(account, accountProof, storageProof, address)
  }

  def getStorageProof(
      account: Account,
      storageKeys: Seq[StorageProofKey]
  ): Either[JsonRpcError, Seq[StorageProof]] = {
    storageKeys.toList
      .map { storageKey =>
        blockchain
          .getStorageProofAt(
            rootHash = account.storageRoot,
            position = storageKey.v,
            ethCompatibleStorage = ethCompatibleStorage
          )
          .map { case (value, proof) => StorageProof(storageKey, value, proof.map(asRlpSerializedNode)) }
          .toRight(noStorageProof(account, storageKey))
      }
      .sequence
      .map(_.toSeq)
  }

  private def noStorageProof(account: Account, storagekey: StorageProofKey): JsonRpcError =
    JsonRpcError.LogicError(s"No storage proof for [${account.toString}] storage key [${storagekey.toString}]")

  private def noAccount(address: Address, blockNumber: BigInt): JsonRpcError =
    JsonRpcError.LogicError(s"No storage proof for Address [${address.toString}] blockNumber [${blockNumber.toString}]")

  private def noAccountProof(address: Address, blockNumber: BigInt): JsonRpcError =
    JsonRpcError.LogicError(s"No storage proof for Address [${address.toString}] blockNumber [${blockNumber.toString}]")

  private def mkAccountProof(
      account: Account,
      accountProof: Seq[ByteString],
      storageProof: Seq[StorageProof],
      address: Address
  ): ProofAccount =
    ProofAccount(
      address = address,
      accountProof = accountProof,
      balance = account.balance,
      codeHash = account.codeHash,
      nonce = account.nonce,
      storageHash = account.storageRoot,
      storageProof = storageProof
    )

  private def asRlpSerializedNode(node: MptNode): ByteString =
    ByteString(MptTraversals.encodeNode(node))

  private def resolveBlock(blockParam: BlockParam): Either[JsonRpcError, ResolvedBlock] = {
    def getBlock(number: BigInt): Either[JsonRpcError, Block] = {
      blockchain
        .getBlockByNumber(number)
        .map(Right.apply)
        .getOrElse(Left(JsonRpcError.InvalidParams(s"Block $number not found")))
    }

    blockParam match {
      case BlockParam.WithNumber(blockNumber) => getBlock(blockNumber).map(ResolvedBlock(_, pendingState = None))
      case BlockParam.Earliest => getBlock(0).map(ResolvedBlock(_, pendingState = None))
      case BlockParam.Latest => getBlock(blockchain.getBestBlockNumber()).map(ResolvedBlock(_, pendingState = None))
      case BlockParam.Pending =>
        blockGenerator.getPendingBlockAndState
          .map(pb => ResolvedBlock(pb.pendingBlock.block, pendingState = Some(pb.worldState)))
          .map(Right.apply)
          .getOrElse(resolveBlock(BlockParam.Latest)) //Default behavior in other clients
    }
  }
}
