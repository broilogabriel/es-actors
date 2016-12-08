package com.broilogabriel

import java.util.UUID

//@SerialVersionUID
object ClusterConfig {
  val scrollSize = 100000
  val minutesAlive = 10
  val bulkActions = 25000
  val bulkSizeMb = 25
  val flushIntervalSec = 5
}

case class ClusterNode(address: String, port: Int)

case class ClusterConfig(name: String, nodes: Seq[ClusterNode], totalHits: Long = 0)

//@SerialVersionUID
case class TransferObject(uuid: UUID, index: String, hitType: String, hitId: String, source: String)

//@SerialVersionUID(1L)
object MORE extends Serializable

//@SerialVersionUID(2L)
object DONE extends Serializable