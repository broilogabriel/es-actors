package com.broilogabriel

import java.util.UUID

@SerialVersionUID(1000L)
case class Cluster(name: String, address: String, port: Int, totalHits: Long = 0)

@SerialVersionUID(2000L)
case class TransferObject(uuid: UUID, index: String, hitType: String, hitId: String, source: String)

@SerialVersionUID(1L)
object MORE extends Serializable

@SerialVersionUID(2L)
object DONE extends Serializable

