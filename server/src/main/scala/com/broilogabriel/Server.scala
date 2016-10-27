package com.broilogabriel

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import com.broilogabriel.ControlMessages.DONE
import com.broilogabriel.ControlMessages.MORE
import com.typesafe.scalalogging.LazyLogging
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexRequest

/**
  * Created by broilogabriel on 21/10/16.
  */
object Server extends App with LazyLogging {
  logger.info(s"${BuildInfo.name} - ${BuildInfo.version}")
  val actorSystem = ActorSystem.create("MigrationServer")
  val actor = actorSystem.actorOf(Props[Server], name = "RemoteServer")
}

class Server extends Actor with LazyLogging {

  def receive = {

    case cluster: Cluster =>
      val uuid = UUID.randomUUID
      logger.info(s"Received cluster config: $cluster")
      context.actorOf(Props(classOf[BulkHandler], cluster), name = uuid.toString).forward(uuid)

    case other => logger.info(s"Unknown message: $other")

  }

}

class BulkHandler(cluster: Cluster) extends Actor with BulkListener with LazyLogging {

  val c = Cluster.getCluster(cluster)
  val bulkProcessor = Cluster.getBulkProcessor(this).build()

  override def client = c

  override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit = {
    super.afterBulk(executionId, request, response)
  }

  override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit = {
    super.afterBulk(executionId, request, failure)
  }

  override def postStop(): Unit = {
    logger.info(s"Stopping actor ${self.path.name}")
    bulkProcessor.flush()
    c.close()
  }

  def receive = {

    case uuid: UUID =>
      logger.info(s"It's me ${uuid.toString}")
      sender() ! uuid

    case data: TransferObject =>
      val indexRequest = new IndexRequest(data.index, data.hitType, data.hitId)
      indexRequest.source(data.source)
      bulkProcessor.add(indexRequest)

    case DONE =>
      sender() ! MORE

    case some: Int =>
      logger.info(s"Client sent $some, sending PoisonPill now")
      sender() ! PoisonPill


    case other => logger.info(s"Something else here? $other")
  }

}
