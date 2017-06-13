package com.broilogabriel

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit, TestProbe }
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll, WordSpecLike }

import scala.concurrent.duration._

class ServerTest extends TestKit(ActorSystem("MySpec")) with LazyLogging with ImplicitSender
    with WordSpecLike with InMemoryES with BeforeAndAfterAll with BeforeAndAfter {

  override def afterAll(): Unit = {
    node.close()
    TestKit.shutdownActorSystem(system)
  }

  after {
    deleteAllData()
  }

  "Server" should {
    "connect to ES" in {
      val actorRef = TestActorRef[Server]
      val clusterConfig = ClusterConfig("elasticsearch_embedded", Seq("localhost"), 9300, 1)
      actorRef ! clusterConfig
      expectMsgClass(classOf[UUID])
    }
    "should persist messages" in {
      val numMesages = 2000
      val actorRef = TestActorRef[Server]
      val clusterConfig = ClusterConfig("elasticsearch_embedded", Seq("localhost"), 9300, numMesages)
      actorRef ! clusterConfig
      expectMsgClass(classOf[UUID])

      val childActor = actorRef.children.toList.head
      val deathWatcher = TestProbe()
      deathWatcher.watch(childActor)

      val sources = 1 to numMesages map { num: Int => generateJson(num) }
      sources.zipWithIndex.foreach {
        case (source, i) => {
          val uuid = UUID.randomUUID().toString
          childActor ! TransferObject(UUID.randomUUID(), "index-1", "type", uuid, source)
          if ((((i + 1) % ClusterConfig.bulkActions) == 0) && ((i + 1) != numMesages)) {
            expectMsg(uuid)
            expectMsg(MORE)
          } else {
            expectMsg(uuid)
          }
        }
      }

      deathWatcher.expectTerminated(childActor, (ClusterConfig.flushIntervalSec + 1) seconds)
    }
  }
}
