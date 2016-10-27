package com.broilogabriel

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.search.SearchHit
import org.joda.time.{DateTime, DateTimeConstants}
import scopt.OptionParser

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by broilogabriel on 24/10/16.
  */
case class Config(index: String = "", indices: Set[String] = Set.empty,
                  source: String = "localhost", sourcePort: Int = 9300, sourceCluster: String = "",
                  target: String = "localhost", targetPort: Int = 9301, targetCluster: String = "",
                  remoteAddress: String = "127.0.0.1", remotePort: Int = 9087, remoteName: String = "RemoteServer")

object Client {
  val name: String = com.broilogabriel.BuildInfo.name
  val version: String = com.broilogabriel.BuildInfo.version

  def indicesByRange(startDate: String, endDate: String, validate: Boolean = false): Option[Set[String]] = {
    try {
      val sd = DateTime.parse(startDate).withDayOfWeek(DateTimeConstants.SUNDAY)
      println(sd)
      val ed = DateTime.parse(endDate).withDayOfWeek(DateTimeConstants.SUNDAY)
      println(ed)
      if (sd.getMillis < ed.getMillis) {
        Some(if (!validate) getIndices(sd, ed) else Set.empty)
      } else {
        None
      }
    } catch {
      case e: IllegalArgumentException => None
    }
  }

  @tailrec
  def getIndices(startDate: DateTime, endDate: DateTime, indices: Set[String] = Set.empty): Set[String] = {
    if (startDate.getMillis > endDate.getMillis) {
      indices
    } else {
      getIndices(startDate.plusWeeks(1), endDate, indices + s"a-${startDate.getWeekyear}-${startDate.getWeekOfWeekyear}")
    }
  }

  def parser: OptionParser[Config] = new OptionParser[Config]("es-client") {
    head(Client.name, Client.version)

    opt[Seq[String]]('i', "indices").valueName("<index1>,<index2>...")
      .action((x, c) => c.copy(indices = x.toSet))
    opt[(String, String)]('d', "dateRange").validate(
      d => if (indicesByRange(d._1, d._2, validate = true).isDefined) success else failure("Invalid dates")
    ).action({
      case ((start, end), c) => c.copy(indices = indicesByRange(start, end).get)
    }).keyValueName("<start_date>", "<end_date>").text("Start date value should be lower than end date.")

    opt[String]('s', "source").valueName("<source_address>")
      .action((x, c) => c.copy(source = x)).text("default value 'localhost'")
    opt[Int]('p', "sourcePort").valueName("<source_port>")
      .action((x, c) => c.copy(sourcePort = x)).text("default value 9300")
    opt[String]('c', "sourceCluster").required().valueName("<source_cluster>")
      .action((x, c) => c.copy(sourceCluster = x))

    opt[String]('t', "target").valueName("<target_address>")
      .action((x, c) => c.copy(target = x)).text("default value 'localhost'")
    opt[Int]('r', "targetPort").valueName("<target_port>")
      .action((x, c) => c.copy(targetPort = x)).text("default value 9301")
    opt[String]('u', "targetCluster").required().valueName("<target_cluster>")
      .action((x, c) => c.copy(targetCluster = x))

    opt[String]("remoteAddress").valueName("<remote_address>").action((x, c) => c.copy(remoteAddress = x))

    help("help").text("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    //    parser.parse(args, Config()) match {
    //      case Some(config) => if (config.indices.nonEmpty) {
    //        init(config)
    //      } else {
    //        println("Missing indices. Check help to send index")
    //      }
    //      case None => println("Try again with the arguments")
    //    }

    init(Config(indices = Set("ASD")))
  }

  def init(config: Config): Unit = {
    val actorSystem = ActorSystem.create("MigrationClient")
    println(s"Creating actors for indices ${config.indices}")
    config.indices.foreach(index =>
      actorSystem.actorOf(Props(classOf[Client], config.copy(index = index, indices = Set.empty)), s"RemoteClient-$index")
    )
  }

}

class Client(config: Config) extends Actor {

  implicit val timeout = Timeout(50, TimeUnit.SECONDS)

  override def preStart(): Unit = {
    val path = s"akka.tcp://MigrationServer@${config.remoteAddress}:${config.remotePort}/user/${config.remoteName}"
    val remote = context.actorSelection(path)
    remote ! Cluster(config.targetCluster, config.target, config.targetPort)
  }

  override def postStop(): Unit = {
    println("Requested to stop. Will terminate the context.")
    context.system.terminate()
  }

  def receive = {

    case uuid: UUID =>
      println(s"Server is waiting to process $uuid")
      //      val cluster = Cluster.getCluster(Cluster(config.sourceCluster, config.source, config.sourcePort))
      //      if (Cluster.checkIndex(cluster, config.index)) {
      //        val scrollId = Cluster.getScrollId(cluster, config.index)
      scroll(System.currentTimeMillis(), null, config.index, null, sender(), uuid).onComplete(
        totalSent => {
          sender() ! totalSent
          println("Client done should wait for server.")
        }
      )
    //      } else {
    //        println(s"Invalid index ${config.index}")
    //        sender() ! s"Invalid index ${config.index}"
    //        self ! PoisonPill
    //      }

  }

  def formatElapsedTime(millis: Long): String = {
    val hours = MILLISECONDS.toHours(millis)
    val minutes = MILLISECONDS.toMinutes(millis)
    f"$hours%02d:${minutes - HOURS.toMinutes(hours)}%02d:${MILLISECONDS.toSeconds(millis) - MINUTES.toSeconds(minutes)}%02d"
  }

  def send(actor: ActorRef, uuid: UUID, hits: Array[SHit]): Future[Unit] = {
    if (hits.nonEmpty) {
      val hit = hits.head
      (actor ? TransferObject(uuid, hit.getIndex, hit.getType, hit.getId, hit.getSourceAsString)).flatMap(
        response => {
          println(s"R: $response")
          send(actor, uuid, hits.tail)
        }
      )
    } else {
      Future(println("Done"))
    }

  }

  private def scroll(startTime: Long, cluster: TransportClient, index: String, scrollId: String, actor: ActorRef,
                     uuid: UUID, total: Int = 0): Future[Int] = {
    val hits = Array[SHit](
      SHit("1", "1", "1", "1"),
      SHit("2", "2", "2", "2"),
      SHit("23", "23", "23", "23")
    )
    //    val hits = Cluster.scroller(index, scrollId, cluster)
    if (hits.nonEmpty) {
      send(actor, uuid, hits).flatMap(x => {
        val sent = hits.length + total
        println(s"Sent $sent in ${formatElapsedTime(System.currentTimeMillis() - startTime)}")
        scroll(startTime, cluster, index, scrollId, actor, uuid, sent)
      })
    } else {
      Future(total)
    }
  }

}

case class SHit(getIndex: String, getType: String, getId: String, getSourceAsString: String)