package com.broilogabriel

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import com.typesafe.scalalogging.LazyLogging
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.index.IndexRequest

object Server extends App with LazyLogging {
  logger.info(s"${BuildInfo.name} - ${BuildInfo.version}")
  val actorSystem = ActorSystem.create("MigrationServer")
  val actor = actorSystem.actorOf(Props[Server], name = "RemoteServer")
}

class Server extends Actor with LazyLogging {

  override def receive: Actor.Receive = {

    case cluster: ClusterConfig =>
      val uuid = UUID.randomUUID
      logger.info(s"Server received cluster config: $cluster")
      context.actorOf(
        Props(classOf[BulkHandler], cluster),
        name = uuid.toString
      ).forward(uuid)

    case other =>
      logger.info(s"Server unknown message: $other")

  }

}

class BulkHandler(cluster: ClusterConfig) extends Actor with LazyLogging {

  var bListener: BulkListener = _
  var bulkProcessor: BulkProcessor = _
  var finishedActions: AtomicLong = _
  var client: ActorRef = _

  override def preStart(): Unit = {
    bListener = BulkListener(Cluster.getCluster(cluster), self)
    bulkProcessor = Cluster.getBulkProcessor(bListener).build()
    finishedActions = new AtomicLong
  }

  override def postStop(): Unit = {
    client ! PoisonPill
    logger.info(s"${self.path.name} - Stopping BulkHandler")
    bulkProcessor.flush()
    bListener.client.close()
  }

  override def receive: Actor.Receive = {

    case uuid: UUID =>
      logger.info(s"${self.path.name} - Starting")
      client = sender()
      client ! uuid

    case to: TransferObject =>
      val indexRequest = new IndexRequest(to.index, to.hitType, to.hitId)
      indexRequest.source(to.source)
      bulkProcessor.add(indexRequest)
      sender ! to.hitId

    case finished: Int =>
      val actions = finishedActions.addAndGet(finished)
      logger.info(
        s"${self.path.name} - Processed ${(actions * 100) / cluster.totalHits}% $actions of ${cluster.totalHits}"
      )
      if (actions < cluster.totalHits) {
        client ! MORE
      } else {
        self ! PoisonPill
      }

    case other =>
      logger.info(s"${self.path.name} - Unknown message: $other")
  }

}
