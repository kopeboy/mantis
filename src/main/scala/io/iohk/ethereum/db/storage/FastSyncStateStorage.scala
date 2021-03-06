package io.iohk.ethereum.db.storage

import java.nio.ByteBuffer

import akka.util.ByteString
import boopickle.CompositePickler
import boopickle.Default._
import io.iohk.ethereum.blockchain.sync.fast.FastSync._
import io.iohk.ethereum.db.dataSource.DataSource
import io.iohk.ethereum.utils.ByteUtils.compactPickledBytes

import scala.collection.immutable.ArraySeq

object FastSyncStateStorage {

  val syncStateKey: String = "fast-sync-state"

}

class FastSyncStateStorage(val dataSource: DataSource)
    extends KeyValueStorage[String, SyncState, FastSyncStateStorage] {
  type T = FastSyncStateStorage

  import FastSyncStateStorage._

  override val namespace: IndexedSeq[Byte] = Namespaces.FastSyncStateNamespace

  implicit val byteStringPickler: Pickler[ByteString] =
    transformPickler[ByteString, Array[Byte]](ByteString(_))(_.toArray[Byte])

  implicit val hashTypePickler: CompositePickler[HashType] =
    compositePickler[HashType]
      .addConcreteType[StateMptNodeHash]
      .addConcreteType[ContractStorageMptNodeHash]
      .addConcreteType[EvmCodeHash]
      .addConcreteType[StorageRootHash]

  override def keySerializer: String => IndexedSeq[Byte] = k =>
    ArraySeq.unsafeWrapArray(k.getBytes(StorageStringCharset.UTF8Charset))

  override def keyDeserializer: IndexedSeq[Byte] => String = b =>
    new String(b.toArray, StorageStringCharset.UTF8Charset)

  override def valueSerializer: SyncState => IndexedSeq[Byte] = ss => compactPickledBytes(Pickle.intoBytes(ss))

  override def valueDeserializer: IndexedSeq[Byte] => SyncState =
    (bytes: IndexedSeq[Byte]) => Unpickle[SyncState].fromBytes(ByteBuffer.wrap(bytes.toArray[Byte]))

  protected def apply(dataSource: DataSource): FastSyncStateStorage = new FastSyncStateStorage(dataSource)

  def putSyncState(syncState: SyncState): FastSyncStateStorage = put(syncStateKey, syncState)

  def getSyncState(): Option[SyncState] = get(syncStateKey)

  def purge(): FastSyncStateStorage = remove(syncStateKey)

}
