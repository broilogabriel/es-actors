package com.broilogabriel

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll, WordSpecLike }

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
      val clusterConfig = ClusterConfig("elasticsearch_embedded", Seq("localhost"), 9300, 1000)
      actorRef ! clusterConfig
      expectMsgClass(classOf[UUID])
    }
    "should shutdown gracefully" in {
      val actorRef = TestActorRef[Server]
      val clusterConfig = ClusterConfig("elasticsearch_embedded", Seq("localhost"), 9300, 1000)
      actorRef ! clusterConfig
      expectMsgClass(classOf[UUID])
      val kids = actorRef.children.toList
      kids.foreach {
        actor => {
          actor ! DONE
        }
      }
    }
    "should persist messages" in {
      val actorRef = TestActorRef[Server]
      val clusterConfig = ClusterConfig("elasticsearch_embedded", Seq("localhost"), 9300, 1500)
      actorRef ! clusterConfig
      expectMsgClass(classOf[UUID])
      val kids = actorRef.children.toList
      val sources = 1 to 1500 map { num: Int => generateJson(num) }
      kids.foreach {
        actor => {
          sources foreach {
            source => {
              val uuid = UUID.randomUUID().toString
              actor ! TransferObject(UUID.randomUUID(), "index-1", "type", uuid, source)
              expectMsg(uuid)
            }
          }
        }
      }
    }
  }
}
