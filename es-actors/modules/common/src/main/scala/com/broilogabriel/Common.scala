package com.broilogabriel

import java.util.UUID

//@SerialVersionUID
object ClusterConfig {
  //Beware of setting this variable any higher, causes memory issues in source cluster
  val scrollSize = 1000
  val minutesAlive = 10
  val bulkActions = 1000
  val bulkSizeMb = 1
  val flushIntervalSec = 5
}

case class ClusterConfig(name: String, addresses: Seq[String], port: Int, totalHits: Long = 0)

//@SerialVersionUID
case class TransferObject(uuid: UUID, index: String, hitType: String, hitId: String, source: String)

//@SerialVersionUID(1L)
object MORE extends Serializable